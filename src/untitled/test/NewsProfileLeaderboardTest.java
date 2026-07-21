import controller.ApplicationController;
import controller.LeaderBoardController;
import controller.NewsController;
import controller.ProfileController;
import model.User.AccountResult;
import model.User.AccountService;
import model.User.AccountStatus;
import model.User.LeaderboardEntry;
import model.User.LeaderboardSortField;
import model.User.PasswordHasher;
import model.User.ProfileInformation;
import model.User.User;
import model.User.UserGender;
import model.User.UserRepository;
import org.junit.Test;
import view.LeaderBoardView;
import view.NewsView;
import view.ProfileView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NewsProfileLeaderboardTest {
    @Test
    public void unreadNewsIsOnlyReturnedOnce() {
        User user = this.createUser("shayan");
        user.addNews("New plant unlocked: Sunflower");
        NewsController controller = new NewsController(new NewsView(), user);

        assertTrue(controller.hasUnreadNews());
        assertEquals(1, controller.showUnreadNews().size());
        assertFalse(controller.hasUnreadNews());
        assertTrue(controller.showUnreadNews().isEmpty());
        assertEquals(1, controller.showAllNews().size());
    }

    @Test
    public void encounteredZombieCreatesOneNewsItem() {
        User user = this.createUser("shayan");

        assertTrue(user.recordEncounteredZombie("basic"));
        assertFalse(user.recordEncounteredZombie("BASIC"));
        assertEquals(1, user.getUnreadNews().size());
        assertTrue(user.getUnreadNews().get(0).contains("basic"));
    }

    @Test
    public void profileChangesUseDistinctValidationResults() throws Exception {
        UserRepository repository = this.createRepository();
        User user = this.createUser("shayan");
        assertTrue(repository.add(user));
        AccountService accountService = new AccountService(repository);
        ProfileController controller = new ProfileController(
                new ProfileView(),
                accountService,
                user
        );

        assertEquals(
                AccountStatus.USERNAME_UNCHANGED,
                controller.changeUsername("SHAYAN").getStatus()
        );
        assertEquals(
                AccountStatus.NICKNAME_UNCHANGED,
                controller.changeNickname(user.getNickname()).getStatus()
        );
        assertEquals(
                AccountStatus.EMAIL_UNCHANGED,
                controller.changeEmail("SHAYAN@example.com").getStatus()
        );
        assertEquals(
                AccountStatus.OLD_PASSWORD_INCORRECT,
                controller.changePassword("Newpass2@", "wrong").getStatus()
        );
        assertEquals(
                AccountStatus.PASSWORD_UNCHANGED,
                controller.changePassword("Oldpass1!", "Oldpass1!").getStatus()
        );

        AccountResult result = controller.changeUsername("new-shayan");
        assertTrue(result.isSuccessful());
        assertNotNull(repository.findByUsername("new-shayan"));
        assertTrue(controller.changeNickname("new nickname").isSuccessful());
        assertTrue(controller.changeEmail("new@example.com").isSuccessful());
        assertTrue(controller.changePassword("Newpass2#", "Oldpass1!").isSuccessful());
        assertEquals(PasswordHasher.hash("Newpass2#"), user.getHashedPassword());
    }

    @Test
    public void profileInformationContainsDocumentedFields() {
        User user = this.createUser("shayan");
        user.setGamesPlayed(7);
        user.setGold(1200);
        user.setDiamond(14);
        user.setLevel(5);
        user.setMaxObtainedMeowPoints(900);

        ProfileInformation information = new ProfileInformation(user);

        assertEquals("shayan", information.getUsername());
        assertEquals(7, information.getGamesPlayed());
        assertEquals(1200, information.getCoins());
        assertEquals(14, information.getDiamonds());
        assertEquals(4, information.getCompletedLevels());
        assertEquals(900, information.getMaximumMeowPoints());
    }

    @Test
    public void leaderboardSortsUsersByMaximumMeowPoints() throws Exception {
        UserRepository repository = this.createRepository();
        User first = this.createUser("first");
        User second = this.createUser("second");
        User third = this.createUser("third");
        first.setMaxObtainedMeowPoints(400);
        second.setMaxObtainedMeowPoints(900);
        third.setMaxObtainedMeowPoints(600);
        repository.add(first);
        repository.add(second);
        repository.add(third);

        LeaderBoardController controller = new LeaderBoardController(
                new LeaderBoardView(),
                new AccountService(repository)
        );
        List<LeaderboardEntry> entries = controller.showLeaderboard();

        assertEquals(3, entries.size());
        assertEquals("second", entries.get(0).getUsername());
        assertEquals("third", entries.get(1).getUsername());
        assertEquals("first", entries.get(2).getUsername());
        assertEquals(1, entries.get(0).getRank());
    }

    @Test
    public void leaderboardSupportsDocumentedColumnsAndBothSortDirections() throws Exception {
        UserRepository repository = this.createRepository();
        User first = this.createUser("first");
        User second = this.createUser("second");
        first.setCompletedDailyQuests(2);
        first.setCompletedNonDailyQuests(4);
        first.setCompletedMinigames(1);
        second.setCompletedDailyQuests(5);
        second.setCompletedNonDailyQuests(1);
        second.setCompletedMinigames(3);
        repository.add(first);
        repository.add(second);

        LeaderBoardController controller = new LeaderBoardController(
                new LeaderBoardView(),
                new AccountService(repository)
        );

        List<LeaderboardEntry> ascending = controller.showLeaderboard(
                LeaderboardSortField.DAILY_QUESTS,
                true
        );
        List<LeaderboardEntry> descending = controller.showLeaderboard(
                LeaderboardSortField.DAILY_QUESTS,
                false
        );

        assertEquals("first", ascending.get(0).getUsername());
        assertEquals("second", descending.get(0).getUsername());
        assertEquals(3, descending.get(0).getCompletedMinigames());
        assertEquals(1, descending.get(0).getCompletedNonDailyQuests());
    }

    @Test
    public void applicationRoutesNewsProfileAndLeaderboardCommands() throws Exception {
        UserRepository repository = this.createRepository();
        User user = this.createUser("shayan");
        user.addNews("New level unlocked");
        user.setMaxObtainedMeowPoints(500);
        repository.add(user);
        ApplicationController controller = new ApplicationController(repository);
        Field currentUser = ApplicationController.class.getDeclaredField("currentUser");
        currentUser.setAccessible(true);
        currentUser.set(controller, user);
        controller.getMenuContext().login();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previousOutput = System.out;

        try {
            System.setOut(new PrintStream(output));
            assertEquals("NEWS_MENU", controller.execute("menu enter news"));
            controller.execute("menu news show-unread");
            assertFalse(user.hasUnreadNews());
            controller.execute("menu exit");
            controller.execute("menu enter profile");
            controller.execute("menu profile change-nickname -u newnickname");
            assertEquals("newnickname", user.getNickname());
            controller.execute("menu exit");
            controller.execute("menu leaderboard");
            assertEquals("MEOW_POINT_MENU", controller.execute("menu meow-point"));
        } finally {
            System.setOut(previousOutput);
        }

        String text = output.toString();
        assertTrue(text.contains("New level unlocked"));
        assertTrue(text.contains("Nickname changed"));
        assertTrue(text.contains("Leaderboard"));
        assertTrue(text.contains("500"));
    }

    private UserRepository createRepository() throws Exception {
        Path usersFile = Files.createTempDirectory("pvz-user-menu").resolve("users.json");
        return new UserRepository(usersFile) {
            @Override
            public void save() {
            }
        };
    }

    private User createUser(String username) {
        return new User(
                username,
                PasswordHasher.hash("Oldpass1!"),
                username + " nickname",
                username + "@example.com",
                1,
                "answer",
                UserGender.MALE
        );
    }
}
