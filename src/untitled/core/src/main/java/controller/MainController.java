package controller;

import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import model.User.AccountService;
import model.User.UserRepository;
import view.MainViewObserver;

public class MainController implements MenuController, MainViewObserver {
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

    @Override
    public boolean onOpenGameMenuRequested() {
        return this.tryEnter(MenuType.GAME_MENU);
    }

    @Override
    public boolean onOpenSettingsMenuRequested() {
        return this.tryEnter(MenuType.SETTINGS_MENU);
    }

    @Override
    public boolean onOpenNewsMenuRequested() {
        return this.tryEnter(MenuType.NEWS_MENU);
    }

    @Override
    public boolean onOpenProfileMenuRequested() {
        return this.tryEnter(MenuType.PROFILE_MENU);
    }

    @Override
    public String onLogoutRequested() {
        return this.logout();
    }

    private boolean tryEnter(MenuType destination) {
        try {
            this.menuContext.enterMenu(destination);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}