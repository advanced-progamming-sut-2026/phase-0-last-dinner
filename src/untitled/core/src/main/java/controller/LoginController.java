package controller;

import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import model.User.AccountResult;
import model.User.AccountService;
import model.User.User;
import model.User.UserRepository;

public class LoginController implements MenuController {
    private final AccountService accountService;
    private final GameMenuContext menuContext;
    private User currentUser;

    public LoginController() {
        this(new AccountService(new UserRepository()), new GameMenuContext());
    }

    public LoginController(AccountService accountService, GameMenuContext menuContext) {
        if (accountService == null || menuContext == null) {
            throw new IllegalArgumentException("Account service and menu context are required");
        }

        this.accountService = accountService;
        this.menuContext = menuContext;
    }

    public AccountResult login(String username, String password, boolean stayLoggedIn) {
        this.accountService.clearPendingRegistration();
        this.accountService.clearPendingPasswordReset();
        AccountResult result = this.accountService.login(username, password, stayLoggedIn);

        if (result.isSuccessful()) {
            this.currentUser = result.getUser();
            this.menuContext.login();
        }

        return result;
    }

    public AccountResult beginPasswordRecovery(String username, String email) {
        return this.accountService.beginPasswordReset(username, email);
    }

    public void cancelPendingPasswordRecovery() {
        this.accountService.clearPendingPasswordReset();
    }

    public AccountResult answerSecurityQuestion(String answer) {
        return this.accountService.verifyPasswordResetAnswer(answer);
    }

    public AccountResult setNewPassword(String newPassword, String newPasswordConfirm) {
        return this.accountService.completePasswordReset(newPassword, newPasswordConfirm);
    }

    public User restoreRememberedLogin() {
        User user = this.accountService.getRememberedUser();

        if (user != null && user.isStayLoggedIn()) {
            this.currentUser = user;
            this.menuContext.login();
        }

        return this.currentUser;
    }

    public GameMenuContext getMenuContext() {
        return this.menuContext;
    }


    public User getCurrentUser() {
        return this.currentUser;
    }

    @Override
    public void changeMenu() {
        this.menuContext.exitMenu();
    }

    @Override
    public MenuType getCurrentMenu() {
        return this.menuContext.getCurrentMenu();
    }
}
