package controller;

import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import model.User.AccountService;
import model.User.UserRepository;

public class MainController implements MenuController {
    private final AccountService accountService;
    private final GameMenuContext menuContext;

    public MainController() {
        this(new AccountService(new UserRepository()), new GameMenuContext());
    }

    public MainController(AccountService accountService, GameMenuContext menuContext) {
        if (accountService == null || menuContext == null) {
            throw new IllegalArgumentException("Account service and menu context are required");
        }

        this.accountService = accountService;
        this.menuContext = menuContext;
    }

    public String logout() {
        this.accountService.logout();
        this.menuContext.logout();
        return "Logout successful";
    }

    @Override
    public void changeMenu() {
        throw new IllegalStateException("Use logout to leave main menu");
    }

    @Override
    public MenuType getCurrentMenu() {
        return this.menuContext.getCurrentMenu();
    }
}
