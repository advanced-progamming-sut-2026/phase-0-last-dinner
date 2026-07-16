import controller.ApplicationController;
import model.Menu.MenuType;
import model.User.UserRepository;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApplicationControllerTest {
    private Path storagePath;
    private ApplicationController controller;

    @Before
    public void setUp() throws IOException {
        this.storagePath = Files.createTempDirectory("pvz-command-users").resolve("users.json");
        this.controller = new ApplicationController(new UserRepository(this.storagePath));
    }

    @Test
    public void runsRegistrationLoginNavigationAndLogoutCommands() {
        assertEquals("SIGNUP_MENU", this.controller.execute("menu show current"));
        assertTrue(this.controller.execute("menu enter main").contains("Cannot enter"));

        String questions = this.controller.execute(
                "register -u shayan -p Strong#123 Strong#123 "
                        + "-n Shayan -e shayan@example.com -g male"
        );
        assertTrue(questions.contains("Pick a security question"));
        assertTrue(questions.contains("1 What was the name of your first pet?"));

        assertEquals(
                "Registration completed",
                this.controller.execute("pick question -q 1 -a blue -c blue")
        );
        assertEquals(MenuType.LOGIN_MENU, this.controller.getCurrentMenu());

        assertEquals(
                "Login successful",
                this.controller.execute("login -u shayan -p Strong#123 -stay-logged-in")
        );
        assertEquals(MenuType.MAIN_MENU, this.controller.getCurrentMenu());
        assertNotNull(this.controller.getCurrentUser());

        assertEquals("GAME_MENU", this.controller.execute("menu enter game"));
        assertEquals("COLLECTION_MENU", this.controller.execute("menu enter collection"));
        assertEquals("GAME_MENU", this.controller.execute("menu exit"));
        assertEquals("MAIN_MENU", this.controller.execute("menu exit"));
        assertEquals("Logout successful", this.controller.execute("menu logout"));
        assertEquals(MenuType.SIGNUP_MENU, this.controller.getCurrentMenu());
    }

    @Test
    public void restoresStayLoggedInAndRunsPasswordRecoveryCommands() {
        this.registerAndLogin(true);

        ApplicationController restored = new ApplicationController(new UserRepository(this.storagePath));
        assertEquals(MenuType.MAIN_MENU, restored.getCurrentMenu());
        assertNotNull(restored.getCurrentUser());

        assertEquals("Logout successful", restored.execute("menu logout"));
        assertEquals("LOGIN_MENU", restored.execute("menu enter login"));

        String question = restored.execute("forget password -u shayan -e shayan@example.com");
        assertTrue(question.contains("What was the name of your first pet?"));
        assertEquals("Enter a new password", restored.execute("answer -a blue"));
        assertEquals(
                "Password changed",
                restored.execute("new password -p NewPass#456 NewPass#456")
        );
        assertEquals(
                "Login successful",
                restored.execute("login -u shayan -p NewPass#456")
        );
    }

    private void registerAndLogin(boolean stayLoggedIn) {
        this.controller.execute(
                "register -u shayan -p Strong#123 Strong#123 "
                        + "-n Shayan -e shayan@example.com -g male"
        );
        this.controller.execute("pick question -q 1 -a blue -c blue");
        this.controller.execute(
                "login -u shayan -p Strong#123" + (stayLoggedIn ? " -stay-logged-in" : "")
        );
    }
}
