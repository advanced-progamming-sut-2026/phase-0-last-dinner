package network.server;

import com.google.gson.JsonObject;
import controller.LeaderBoardController;
import model.User.AccountResult;
import model.User.AccountStatus;
import model.User.LeaderboardEntry;
import model.User.LeaderboardSortField;
import model.User.LeaderboardUnavailableException;
import model.User.NetworkAccountService;
import model.User.User;
import model.User.UserGender;
import network.client.GameClient;
import network.protocol.NetworkResponse;
import network.protocol.RequestType;
import org.junit.Test;
import view.LeaderBoardView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.ServerSocket;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NetworkAccountServiceTest {
    @Test
    public void accountDataIsUniquePersistentAndAvailableToAnotherClient() throws Exception {
        Path directory = Files.createTempDirectory("pvz-server-account-test");
        Path serverData = directory.resolve("users.json");
        Path firstSession = directory.resolve("first-session.json");
        Path secondSession = directory.resolve("second-session.json");

        RequestRouter router = RequestRouter.withDefaults();
        new ServerAccountService(new ServerUserRepository(serverData)).registerRoutes(router);
        try (GameServer server = new GameServer("127.0.0.1", 0, router)) {
            server.start();

            try (NetworkAccountService first = service(server, firstSession)) {
                assertEquals(AccountStatus.SECURITY_QUESTION_REQUIRED,
                        first.beginRegistration(
                                "network-user", "SafePass1!", "SafePass1!",
                                "Peashooter", "user@example.com", UserGender.MALE
                        ).getStatus());
                assertTrue(first.completeRegistration(1, "Fluffy", "Fluffy").isSuccessful());
                AccountResult login = first.login("network-user", "SafePass1!", true);
                assertTrue(login.isSuccessful());
                login.getUser().setGold(420);
                login.getUser().setDiamond(17);
                first.save();
            }

            try (NetworkAccountService second = service(server, secondSession)) {
                second.beginRegistration(
                        "NETWORK-user", "OtherPass1!", "OtherPass1!",
                        "Sunflower", "other@example.com", UserGender.FEMALE);
                assertEquals(AccountStatus.USERNAME_TAKEN,
                        second.completeRegistration(2, "Tehran", "Tehran").getStatus());

                AccountResult login = second.login("network-user", "SafePass1!", false);
                assertTrue(login.isSuccessful());
                assertEquals(420, login.getUser().getGold());
                assertEquals(17, login.getUser().getDiamond());
                assertTrue(second.changeNickname(login.getUser(), "Repeater").isSuccessful());
            }
        }

        String stored = Files.readString(serverData);
        assertFalse(stored.contains("SafePass1!"));
        assertFalse(stored.contains("Fluffy"));

        RequestRouter restartedRouter = RequestRouter.withDefaults();
        new ServerAccountService(new ServerUserRepository(serverData)).registerRoutes(restartedRouter);
        try (GameServer restarted = new GameServer("127.0.0.1", 0, restartedRouter)) {
            restarted.start();
            try (NetworkAccountService restored = service(restarted, directory.resolve("restart-session.json"))) {
                User user = restored.login("network-user", "SafePass1!", false).getUser();
                assertNotNull(user);
                assertEquals("Repeater", user.getNickname());
                assertEquals(420, user.getGold());
            }
        }
    }

    @Test
    public void passwordRecoveryAndProfileChangesAreCheckedByServer() throws Exception {
        Path directory = Files.createTempDirectory("pvz-server-profile-test");
        RequestRouter router = RequestRouter.withDefaults();
        new ServerAccountService(
                new ServerUserRepository(directory.resolve("users.json"))).registerRoutes(router);

        try (GameServer server = new GameServer("127.0.0.1", 0, router)) {
            server.start();
            try (NetworkAccountService account = service(server, directory.resolve("session.json"))) {
                account.beginRegistration(
                        "profile-user", "FirstPass1!", "FirstPass1!",
                        "Wallnut", "first@example.com", UserGender.FEMALE);
                assertTrue(account.completeRegistration(3, "Peashooter", "Peashooter").isSuccessful());
                User user = account.login("profile-user", "FirstPass1!", false).getUser();

                assertEquals(AccountStatus.OLD_PASSWORD_INCORRECT,
                        account.changePassword(user, "SecondPass2!", "wrong").getStatus());
                assertTrue(account.changeEmail(user, "second@example.com").isSuccessful());
                account.logout();

                assertTrue(account.beginPasswordReset("profile-user", "second@example.com").isSuccessful());
                assertEquals(AccountStatus.SECURITY_ANSWER_INVALID,
                        account.verifyPasswordResetAnswer("wrong").getStatus());
                assertTrue(account.beginPasswordReset("profile-user", "second@example.com").isSuccessful());
                assertTrue(account.verifyPasswordResetAnswer("peashooter").isSuccessful());
                assertTrue(account.completePasswordReset("SecondPass2!", "SecondPass2!").isSuccessful());
                assertTrue(account.login("profile-user", "SecondPass2!", false).isSuccessful());
            }
        }
    }

    @Test
    public void rememberedSessionSurvivesServerRestart() throws Exception {
        Path directory = Files.createTempDirectory("pvz-server-session-test");
        Path serverData = directory.resolve("users.json");
        Path sessionPath = directory.resolve("session.json");

        RequestRouter firstRouter = RequestRouter.withDefaults();
        new ServerAccountService(new ServerUserRepository(serverData)).registerRoutes(firstRouter);
        try (GameServer server = new GameServer("127.0.0.1", 0, firstRouter)) {
            server.start();
            try (NetworkAccountService account = service(server, sessionPath)) {
                account.beginRegistration(
                        "remember-user", "Remember1!", "Remember1!",
                        "Bonkchoy", "remember@example.com", UserGender.MALE);
                assertTrue(account.completeRegistration(1, "Milo", "Milo").isSuccessful());
                assertTrue(account.login("remember-user", "Remember1!", true).isSuccessful());
            }
        }

        RequestRouter restartedRouter = RequestRouter.withDefaults();
        new ServerAccountService(new ServerUserRepository(serverData)).registerRoutes(restartedRouter);
        try (GameServer restarted = new GameServer("127.0.0.1", 0, restartedRouter)) {
            restarted.start();
            try (NetworkAccountService account = service(restarted, sessionPath)) {
                User restored = account.getRememberedUser();
                assertNotNull(restored);
                assertEquals("remember-user", restored.getUsername());
            }
        }
    }

    @Test
    public void leaderboardUsesCurrentServerDataWithoutExposingPrivateFields() throws Exception {
        Path directory = Files.createTempDirectory("pvz-server-leaderboard-test");
        ServerUserRepository repository = new ServerUserRepository(directory.resolve("users.json"));
        ServerAccountService serverAccounts = new ServerAccountService(repository);
        RequestRouter router = RequestRouter.withDefaults();
        serverAccounts.registerRoutes(router);
        new ServerLeaderboardService(repository, serverAccounts).registerRoutes(router);

        try (GameServer server = new GameServer("127.0.0.1", 0, router)) {
            server.start();
            try (NetworkAccountService first = service(server, directory.resolve("first.json"));
                 NetworkAccountService second = service(server, directory.resolve("second.json"))) {
                User firstUser = registerAndLogin(
                        first, "alice", "AlicePass1!", "Peashooter", "alice@example.com");
                firstUser.setMaxObtainedMeowPoints(300);
                firstUser.setCompletedMinigames(2);
                first.save();

                User secondUser = registerAndLogin(
                        second, "bob", "BobSecure1!", "Sunflower", "bob@example.com");
                secondUser.setMaxObtainedMeowPoints(900);
                secondUser.setCompletedMinigames(5);
                second.save();

                LeaderBoardController controller = new LeaderBoardController(new LeaderBoardView(), first);
                List<LeaderboardEntry> initial = controller.showLeaderboard(
                        LeaderboardSortField.MEOW_POINTS, false);
                assertEquals(2, initial.size());
                assertEquals("bob", initial.get(0).getUsername());
                assertEquals(1, initial.get(0).getRank());

                firstUser.setMaxObtainedMeowPoints(1200);
                List<LeaderboardEntry> updated = controller.showLeaderboard(
                        LeaderboardSortField.MEOW_POINTS, false);
                assertEquals("alice", updated.get(0).getUsername());
                assertEquals(1200, updated.get(0).getMeowPoints());

                List<LeaderboardEntry> alphabetical = controller.showLeaderboard(
                        LeaderboardSortField.USERNAME, true);
                assertEquals("alice", alphabetical.get(0).getUsername());
                assertEquals("bob", alphabetical.get(1).getUsername());

                try (GameClient rawClient = new GameClient("127.0.0.1", server.getPort())) {
                    JsonObject loginPayload = new JsonObject();
                    loginPayload.addProperty("username", "alice");
                    loginPayload.addProperty("password", "AlicePass1!");
                    NetworkResponse login = rawClient.send(RequestType.LOGIN, loginPayload);
                    JsonObject leaderboardPayload = new JsonObject();
                    leaderboardPayload.addProperty("token", login.getPayload().get("token").getAsString());
                    leaderboardPayload.addProperty("sortField", "MEOW_POINTS");
                    NetworkResponse response = rawClient.send(RequestType.GET_LEADERBOARD, leaderboardPayload);
                    String publicData = response.getPayload().get("entries").toString();
                    assertFalse(publicData.contains("alice@example.com"));
                    assertFalse(publicData.contains("password"));
                    assertFalse(publicData.contains("securityAnswer"));
                }
            }
        }
    }

    @Test
    public void unavailableServerIsNotReportedAsAnEmptyLeaderboard() throws Exception {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }

        Path session = Files.createTempFile("pvz-offline-leaderboard", ".json");
        Files.deleteIfExists(session);
        try (NetworkAccountService account = new NetworkAccountService(
                new GameClient("127.0.0.1", unusedPort), session)) {
            try {
                account.getLeaderboard(LeaderboardSortField.MEOW_POINTS, false);
            } catch (LeaderboardUnavailableException exception) {
                assertTrue(exception.getMessage().contains("reach"));
                return;
            }
        }

        throw new AssertionError("Unavailable leaderboard must report a server error");
    }

    private User registerAndLogin(
            NetworkAccountService account,
            String username,
            String password,
            String nickname,
            String email
    ) {
        account.beginRegistration(
                username, password, password, nickname, email, UserGender.MALE);
        assertTrue(account.completeRegistration(1, "Milo", "Milo").isSuccessful());
        AccountResult login = account.login(username, password, false);
        assertTrue(login.isSuccessful());
        return login.getUser();
    }

    private NetworkAccountService service(GameServer server, Path sessionPath) {
        return new NetworkAccountService(
                new GameClient("127.0.0.1", server.getPort()),
                sessionPath
        );
    }
}
