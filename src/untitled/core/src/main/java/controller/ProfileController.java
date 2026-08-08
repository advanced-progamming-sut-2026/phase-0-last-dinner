package controller;

import model.Menu.MenuType;
import model.User.AccountResult;
import model.User.AccountService;
import model.User.AccountStatus;
import model.User.ProfileInformation;
import model.User.User;
import view.ProfileView;
import view.ProfileViewObserver;

public class ProfileController implements MenuController, ProfileViewObserver {
    private final AccountService accountService;
    private final User user;

    public ProfileController() {
        this.accountService = null;
        this.user = null;
    }

    public ProfileController(ProfileView view, AccountService accountService, User user) {
        if (view == null || accountService == null || user == null) {
            throw new IllegalArgumentException("Profile view account service and user are required");
        }

        user.initializeMissingFields();
        this.accountService = accountService;
        this.user = user;
        view.setObserver(this);
    }

    @Override
    public MenuType getCurrentMenu() {
        return MenuType.PROFILE_MENU;
    }

    @Override
    public void changeMenu() {
    }

    public AccountResult changeUsername(String username) {
        if (this.accountService == null || this.user == null) {
            return AccountResult.failure(
                    AccountStatus.USER_NOT_FOUND,
                    "User is not available"
            );
        }

        return this.accountService.changeUsername(this.user, username);
    }

    public AccountResult changeEmail(String email) {
        if (this.accountService == null || this.user == null) {
            return AccountResult.failure(
                    AccountStatus.USER_NOT_FOUND,
                    "User is not available"
            );
        }

        return this.accountService.changeEmail(this.user, email);
    }

    public AccountResult changeNickname(String nickname) {
        if (this.accountService == null || this.user == null) {
            return AccountResult.failure(
                    AccountStatus.USER_NOT_FOUND,
                    "User is not available"
            );
        }

        return this.accountService.changeNickname(this.user, nickname);
    }

    public AccountResult changePassword(String newPassword, String oldPassword) {
        if (this.accountService == null || this.user == null) {
            return AccountResult.failure(
                    AccountStatus.USER_NOT_FOUND,
                    "User is not available"
            );
        }

        return this.accountService.changePassword(this.user, newPassword, oldPassword);
    }

    public ProfileInformation showInformation() {
        return this.user == null ? null : new ProfileInformation(this.user);
    }

    @Override
    public AccountResult onChangeUsernameRequested(String username) {
        return this.changeUsername(username);
    }

    @Override
    public AccountResult onChangeEmailRequested(String email) {
        return this.changeEmail(email);
    }

    @Override
    public AccountResult onChangeNicknameRequested(String nickname) {
        return this.changeNickname(nickname);
    }

    @Override
    public AccountResult onChangePasswordRequested(String newPassword, String oldPassword) {
        return this.changePassword(newPassword, oldPassword);
    }

    @Override
    public ProfileInformation onShowInformationRequested() {
        return this.showInformation();
    }
}
