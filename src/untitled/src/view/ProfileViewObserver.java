package view;

import model.User.AccountResult;
import model.User.ProfileInformation;

public interface ProfileViewObserver {
    AccountResult onChangeUsernameRequested(String username);

    AccountResult onChangeNicknameRequested(String nickname);

    AccountResult onChangeEmailRequested(String email);

    AccountResult onChangePasswordRequested(String newPassword, String oldPassword);

    ProfileInformation onShowInformationRequested();
}
