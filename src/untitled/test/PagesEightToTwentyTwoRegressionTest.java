import college.java.project.Main;import controller.ApplicationController;
import controller.TravelLogController;
import model.User.User;
import model.User.UserGender;
import model.User.UserRepository;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.minigame.StageProgressMiniGame;
import org.junit.Test;
import view.CommandHandler;
import view.travellog.TravelLogView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PagesEightToTwentyTwoRegressionTest {
    @Test
    public void quotedFlagCanBeUsedAsUsername() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-quoted-user").resolve("users.json");
        UserRepository repository = new UserRepository(usersFile);
        ApplicationController controller = new ApplicationController(
                repository,
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        assertTrue(controller.execute(
                "register -u \"-p\" -p Strong#123 Strong#123 "
                        + "-n Quoted -e quoted@example.com -g male"
        ).contains("Pick a security question"));
        controller.execute("pick question -q 1 -a blue -c blue");

        assertNotNull(repository.findByUsername("-p"));
    }

    @Test
    public void incompleteWorkflowClearsOlderPendingAccountState() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-pending-account").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute(
                "register -u pending -p Strong#123 Strong#123 "
                        + "-n Pending -e pending@example.com -g male"
        );
        controller.execute("register -u incomplete");
        assertTrue(controller.execute("pick question -q 1 -a blue -c blue")
                .contains("Registration information is required first"));
    }

    @Test
    public void leavingSignupClearsPendingRegistration() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-leave-signup").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute(
                "register -u pending -p Strong#123 Strong#123 "
                        + "-n Pending -e pending@example.com -g male"
        );
        assertEquals("LOGIN_MENU", controller.execute("menu enter login"));
        assertEquals("SIGNUP_MENU", controller.execute("menu exit"));
        assertTrue(controller.execute("pick question -q 1 -a blue -c blue")
                .contains("Registration information is required first"));
    }

    @Test
    public void normalLoginClearsVerifiedPasswordReset() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-reset-session").resolve("users.json");
        UserRepository repository = new UserRepository(usersFile);
        this.register(repository, "first", "first@example.com");
        this.register(repository, "second", "second@example.com");
        ApplicationController controller = new ApplicationController(
                repository,
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        assertEquals("LOGIN_MENU", controller.execute("menu enter login"));
        assertTrue(controller.execute("forget password -u first -e first@example.com")
                .contains("What was the name of your first pet"));
        assertEquals("Enter a new password", controller.execute("answer -a blue"));
        assertEquals("Login successful", controller.execute("login -u second -p Strong#123"));
        assertEquals("Logout successful", controller.execute("menu logout"));
        assertEquals("LOGIN_MENU", controller.execute("menu enter login"));
        assertTrue(controller.execute("new password -p Changed#456 Changed#456")
                .contains("Security answer is required first"));
    }

    @Test
    public void incompleteLoginAlsoClearsVerifiedPasswordReset() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-incomplete-login").resolve("users.json");
        UserRepository repository = new UserRepository(usersFile);
        this.register(repository, "first", "first@example.com");
        ApplicationController controller = new ApplicationController(
                repository,
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute("menu enter login");
        controller.execute("forget password -u first -e first@example.com");
        controller.execute("answer -a blue");
        assertEquals("Login command is incomplete", controller.execute("login -u first"));
        assertTrue(controller.execute("new password -p Changed#456 Changed#456")
                .contains("Security answer is required first"));
    }

    @Test
    public void minigameStageUnlockSurvivesReloadAndCreatesNews() throws Exception {
        Path usersFile = Files.createTempDirectory("pvz-minigame-progress").resolve("users.json");
        UserRepository repository = new UserRepository(usersFile);
        User user = this.createUser();
        MiniGame miniGame = user.getTravelLog().findMiniGame(MiniGameType.VASEBREAKER);
        StageProgressMiniGame progress = (StageProgressMiniGame) miniGame;
        progress.restoreHighestUnlockedStage(2);

        assertTrue(user.recordMiniGameStageProgress(MiniGameType.VASEBREAKER, 2));
        assertTrue(user.getUnreadNews().get(0).contains("stage 2"));
        assertTrue(repository.add(user));

        User loaded = new UserRepository(usersFile).findByUsername(user.getUsername());
        StageProgressMiniGame loadedProgress = (StageProgressMiniGame) loaded.getTravelLog()
                .findMiniGame(MiniGameType.VASEBREAKER);
        assertEquals(2, loadedProgress.getHighestUnlockedStage());
    }

    @Test
    public void completedLegacyMinigameRestoresAtFinalStage() throws Exception {
        User user = this.createUser();
        user.recordMiniGameCompletion(MiniGameType.VASEBREAKER);
        this.setField(user, "highestUnlockedMiniGameStages", new HashMap<MiniGameType, Integer>());
        this.setField(user, "travelLog", null);

        user.initializeMissingFields();

        MiniGame miniGame = user.getTravelLog().findMiniGame(MiniGameType.VASEBREAKER);
        StageProgressMiniGame progress = (StageProgressMiniGame) miniGame;
        assertEquals(3, progress.getHighestUnlockedStage());
        assertTrue(miniGame.isAllStagesCompleted());
    }

    @Test
    public void minigameZombiesReachTheUserCollection() {
        Main application = Main.loadApplication();
        User user = this.createUser();
        TravelLogView view = new TravelLogView();
        TravelLogController controller = new TravelLogController(
                view,
                user.getTravelLog(),
                user,
                application.getPlantDefinitions()
        );
        CommandHandler handler = controller.onOpenMiniGameRequested(MiniGameType.ZOMBOTANY);

        handler.handleCommand("zombotany start -s 1");
        handler.handleCommand("add plant -t Peashooter");
        handler.handleCommand("start game");
        handler.handleCommand("zombotany advance -t 1");

        assertFalse(user.getEncounteredZombieAliases().isEmpty());
    }

    private User createUser() {
        return new User(
                "shayan",
                "hash",
                "Shayan",
                "shayan@example.com",
                1,
                "blue",
                UserGender.MALE
        );
    }

    private void register(UserRepository repository, String username, String email) {
        model.User.AccountService service = new model.User.AccountService(repository);
        service.beginRegistration(
                username,
                "Strong#123",
                "Strong#123",
                username,
                email,
                UserGender.MALE
        );
        service.completeRegistration(1, "blue", "blue");
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
