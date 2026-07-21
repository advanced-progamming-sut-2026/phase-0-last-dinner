package controller;

import model.Menu.GameMenuContext;
import model.Menu.MenuType;
import model.User.AccountResult;
import model.User.AccountService;
import model.User.AccountStatus;
import model.User.UserGender;
import model.User.UserRepository;

import java.util.List;
import java.util.Locale;

public class SignupController implements MenuController {
    private final AccountService accountService;
    private final GameMenuContext menuContext;

    public SignupController() {
        this(new AccountService(new UserRepository()), new GameMenuContext());
    }

    public SignupController(AccountService accountService, GameMenuContext menuContext) {
        if (accountService == null || menuContext == null) {
            throw new IllegalArgumentException("Account service and menu context are required");
        }

        this.accountService = accountService;
        this.menuContext = menuContext;
    }

    public AccountResult register(
            String username,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            String gender
    ) {
        this.accountService.clearPendingRegistration();
        UserGender parsedGender = this.parseGender(gender);

        if (parsedGender == null) {
            return AccountResult.failure(AccountStatus.GENDER_INVALID, "Gender must be male or female");
        }

        return this.accountService.beginRegistration(
                username,
                password,
                passwordConfirm,
                nickname,
                email,
                parsedGender
        );
    }

    public AccountResult pickQuestion(int questionNumber, String answer, String answerConfirm) {
        AccountResult result = this.accountService.completeRegistration(
                questionNumber,
                answer,
                answerConfirm
        );

        if (result.isSuccessful()) {
            this.menuContext.enterMenu(MenuType.LOGIN_MENU);
        }

        return result;
    }

    public List<String> getSecurityQuestions() {
        return this.accountService.getSecurityQuestions();
    }

    public void cancelPendingRegistration() {
        this.accountService.clearPendingRegistration();
    }

    @Override
    public MenuType getCurrentMenu() {
        return this.menuContext.getCurrentMenu();
    }

    @Override
    public void changeMenu() {
        this.menuContext.enterMenu(MenuType.LOGIN_MENU);
    }

    private UserGender parseGender(String gender) {
        if (gender == null) {
            return null;
        }

        String normalized = gender.trim().toUpperCase(Locale.ROOT);

        if ("MALE".equals(normalized) || "MAN".equals(normalized)) {
            return UserGender.MALE;
        }

        if ("FEMALE".equals(normalized) || "WOMAN".equals(normalized)) {
            return UserGender.FEMALE;
        }

        return null;
    }
}
