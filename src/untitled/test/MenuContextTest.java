import college.java.project.Main;import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MenuContextTest {
    @Test
    public void followsDocumentedMenuFlow() {
        GameMenuContext context = new GameMenuContext();

        assertEquals(MenuType.SIGNUP_MENU, context.getCurrentMenu());
        context.enterMenu(MenuType.LOGIN_MENU);
        assertEquals(MenuType.LOGIN_MENU, context.getCurrentMenu());

        context.login();
        assertEquals(MenuType.MAIN_MENU, context.getCurrentMenu());
        context.enterMenu(MenuType.GAME_MENU);
        context.enterMenu(MenuType.COLLECTION_MENU);
        context.exitMenu();
        assertEquals(MenuType.GAME_MENU, context.getCurrentMenu());
        context.exitMenu();
        assertEquals(MenuType.MAIN_MENU, context.getCurrentMenu());

        context.logout();
        assertEquals(MenuType.SIGNUP_MENU, context.getCurrentMenu());
        assertFalse(context.isLoggedIn());
    }

    @Test(expected = IllegalStateException.class)
    public void mainMenuCannotBeEnteredWithoutLogin() {
        GameMenuContext context = new GameMenuContext();
        context.enterMenu(MenuType.LOGIN_MENU);
        context.enterMenu(MenuType.MAIN_MENU);
    }

    @Test
    public void exitingSignupStopsApplication() {
        GameMenuContext context = new GameMenuContext();

        assertTrue(context.isApplicationRunning());
        context.exitMenu();
        assertFalse(context.isApplicationRunning());
    }
}
