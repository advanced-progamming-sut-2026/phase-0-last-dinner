package model.User;

import lombok.Getter;

@Getter
// natije yek amaliate account ro yekja negah midare
public class AccountResult {
    private final AccountStatus status;
    private final String message;
    private final User user;
    private final String securityQuestion;

    private AccountResult(
            AccountStatus status,
            String message,
            User user,
            String securityQuestion
    ) {
        this.status = status;
        this.message = message;
        this.user = user;
        this.securityQuestion = securityQuestion;
    }

    public static AccountResult success(String message, User user) {
        return new AccountResult(AccountStatus.SUCCESS, message, user, null);
    }

    public static AccountResult questionRequired(String message) {
        return new AccountResult(AccountStatus.SECURITY_QUESTION_REQUIRED, message, null, null);
    }

    public static AccountResult question(String message, String securityQuestion, User user) {
        return new AccountResult(AccountStatus.SUCCESS, message, user, securityQuestion);
    }

    public static AccountResult failure(AccountStatus status, String message) {
        return new AccountResult(status, message, null, null);
    }

    public boolean isSuccessful() {
        return this.status == AccountStatus.SUCCESS;
    }
}
