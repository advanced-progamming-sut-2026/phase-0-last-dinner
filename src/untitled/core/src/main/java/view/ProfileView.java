package view;

import model.User.AccountResult;
import model.User.ProfileInformation;

import java.util.regex.Matcher;

public class ProfileView implements CommandHandler {
    private ProfileViewObserver observer;

    public void setObserver(ProfileViewObserver observer) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        if (this.observer == null) {
            System.out.println("Profile controller is not connected.");
            return;
        }

        Matcher matcher = ProfileCommand.CHANGE_USERNAME.getMatcher(input);

        if (matcher != null) {
            this.printResult(this.observer.onChangeUsernameRequested(this.clean(matcher.group("username"))));
            return;
        }

        matcher = ProfileCommand.CHANGE_NICKNAME.getMatcher(input);

        if (matcher != null) {
            this.printResult(this.observer.onChangeNicknameRequested(this.clean(matcher.group("nickname"))));
            return;
        }

        matcher = ProfileCommand.CHANGE_EMAIL.getMatcher(input);

        if (matcher != null) {
            this.printResult(this.observer.onChangeEmailRequested(this.clean(matcher.group("email"))));
            return;
        }

        matcher = ProfileCommand.CHANGE_PASSWORD.getMatcher(input);

        if (matcher != null) {
            this.printResult(this.observer.onChangePasswordRequested(
                    this.clean(matcher.group("newPassword")),
                    this.clean(matcher.group("oldPassword"))
            ));
            return;
        }

        matcher = ProfileCommand.SHOW_INFORMATION.getMatcher(input);

        if (matcher != null) {
            this.printInformation(this.observer.onShowInformationRequested());
            return;
        }

        System.out.println("Invalid profile command.");
    }

    private void printResult(AccountResult result) {
        System.out.println(result == null ? "Profile action failed." : result.getMessage());
    }

    private void printInformation(ProfileInformation information) {
        if (information == null) {
            System.out.println("Profile information is not available.");
            return;
        }

        System.out.println("Username: " + information.getUsername());
        System.out.println("Nickname: " + information.getNickname());
        System.out.println("Games played: " + information.getGamesPlayed());
        System.out.println("Coins: " + information.getCoins());
        System.out.println("Diamonds: " + information.getDiamonds());
        System.out.println("Completed levels: " + information.getCompletedLevels());
        System.out.println("Maximum Meow Points: " + information.getMaximumMeowPoints());
    }

    private String clean(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }
}
