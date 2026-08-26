package model.User;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
// marhale haye sabt nam login va avaz kardane ramz ro anjam mide
public class AccountService implements AutoCloseable {
    private static final String SPECIAL_CHARACTERS = "?><,\"';:\\/|[]}{+=()*&^%$#!";
    private static final List<String> SECURITY_QUESTIONS = Collections.unmodifiableList(Arrays.asList(
            "What was the name of your first pet?",
            "In which city were you born?",
            "What is your favorite plant?"
    ));
    private final UserRepository repository;
    // etelaate sabt nam ro ta entekhabe soal negah midare
    private PendingRegistration pendingRegistration;
    // karbari ke dare ramzesho avaz mikone
    private User pendingPasswordReset;
    // neshun mide javabe soal ghablan check shode
    private boolean passwordResetAnswerVerified;
    protected AccountService() {
        this.repository = null;
    }
    public AccountService(UserRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("User repository is required");
        }
        this.repository = repository;
    }
    public List<String> getSecurityQuestions() {
        return SECURITY_QUESTIONS;
    }
    public void clearPendingRegistration() {
        this.pendingRegistration = null;
    }
    public void clearPendingPasswordReset() {
        this.pendingPasswordReset = null;
        this.passwordResetAnswerVerified = false;
    }
    public AccountResult beginRegistration(
            String username,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            UserGender gender
    ) {
        this.pendingRegistration = null;
        AccountResult validation = this.validateRegistration(
                username,
                password,
                passwordConfirm,
                nickname,
                email,
                gender
        );
        if (validation != null) {
            return validation;
        }
        this.pendingRegistration = new PendingRegistration(
                username,
                PasswordHasher.hash(password),
                nickname,
                email,
                gender
        );
        return AccountResult.questionRequired("Pick a security question");
    }
    private AccountResult validateRegistration(
            String username,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            UserGender gender
    ) {
        if (!this.isValidUsername(username)) {
            return AccountResult.failure(
                    AccountStatus.USERNAME_INVALID,
                    "Username may only contain letters numbers and hyphens"
            );
        }
        if (this.repository.findByUsername(username) != null) {
            return AccountResult.failure(AccountStatus.USERNAME_TAKEN, "Username already exists");
        }
        String passwordError = this.getPasswordError(password);
        if (passwordError != null) {
            return AccountResult.failure(AccountStatus.PASSWORD_INVALID, passwordError);
        }
        if (!password.equals(passwordConfirm)) {
            return AccountResult.failure(AccountStatus.PASSWORD_MISMATCH, "Passwords do not match");
        }
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            return AccountResult.failure(
                    AccountStatus.NICKNAME_INVALID,
                    "Nickname must be between 3 and 30 characters"
            );
        }
        if (!this.isValidEmail(email)) {
            return AccountResult.failure(AccountStatus.EMAIL_INVALID, "Email format is invalid");
        }
        if (gender == null) {
            return AccountResult.failure(AccountStatus.GENDER_INVALID, "Gender must be male or female");
        }
        return null;
    }
    public AccountResult completeRegistration(
            int questionNumber,
            String answer,
            String answerConfirm
    ) {
        if (this.pendingRegistration == null) {
            return AccountResult.failure(
                    AccountStatus.SECURITY_QUESTION_INVALID,
                    "Registration information is required first"
            );
        }
        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.size()) {
            return AccountResult.failure(
                    AccountStatus.SECURITY_QUESTION_INVALID,
                    "Security question number is invalid"
            );
        }
        if (answer == null || answer.trim().isEmpty() || !answer.equals(answerConfirm)) {
            return AccountResult.failure(
                    AccountStatus.SECURITY_ANSWER_INVALID,
                    "Security answers do not match"
            );
        }
        User user = new User(
                this.pendingRegistration.username,
                this.pendingRegistration.hashedPassword,
                this.pendingRegistration.nickname,
                this.pendingRegistration.email,
                questionNumber,
                PasswordHasher.hash(this.normalizeAnswer(answer)),
                this.pendingRegistration.gender
        );
        if (!this.repository.add(user)) {
            return AccountResult.failure(AccountStatus.USERNAME_TAKEN, "Username already exists");
        }
        this.pendingRegistration = null;
        return AccountResult.success("Registration completed", user);
    }
    public AccountResult login(String username, String password, boolean stayLoggedIn) {
        User user = this.repository.findByUsername(username);
        if (user == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "Username was not found");
        }
        if (password == null || user.getHashedPassword() == null
                || !user.getHashedPassword().equals(PasswordHasher.hash(password))) {
            return AccountResult.failure(AccountStatus.CREDENTIALS_INVALID, "Password is incorrect");
        }
        user.setStayLoggedIn(stayLoggedIn);
        this.repository.remember(stayLoggedIn ? user : null);
        this.repository.save();
        return AccountResult.success("Login successful", user);
    }
    public List<User> getUsers() {
        return this.repository.getUsers();
    }
    public List<LeaderboardEntry> getLeaderboard(
            LeaderboardSortField sortField,
            boolean ascending
    ) {
        List<User> rankedUsers = new ArrayList<>();
        for (User user : this.getUsers()) {
            if (user != null) {
                rankedUsers.add(user);
            }
        }
        Comparator<User> comparator = this.leaderboardComparator(sortField);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(
                User::getUsername,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
        rankedUsers.sort(comparator);

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < rankedUsers.size(); i++) {
            entries.add(new LeaderboardEntry(i + 1, rankedUsers.get(i)));
        }
        return entries;
    }
    public AccountResult changeUsername(User user, String username) {
        if (user == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "User is not available");
        }
        if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(username)) {
            return AccountResult.failure(
                    AccountStatus.USERNAME_UNCHANGED,
                    "New username must be different from the current username"
            );
        }
        if (!this.isValidUsername(username)) {
            return AccountResult.failure(
                    AccountStatus.USERNAME_INVALID,
                    "Username may only contain letters numbers and hyphens"
            );
        }
        if (this.repository.findByUsername(username) != null) {
            return AccountResult.failure(AccountStatus.USERNAME_TAKEN, "Username already exists");
        }
        user.setUsername(username);
        this.saveProfileChange(user);
        return AccountResult.success("Username changed", user);
    }
    public AccountResult changeNickname(User user, String nickname) {
        if (user == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "User is not available");
        }
        if (user.getNickname() != null && user.getNickname().equals(nickname)) {
            return AccountResult.failure(
                    AccountStatus.NICKNAME_UNCHANGED,
                    "New nickname must be different from the current nickname"
            );
        }
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            return AccountResult.failure(
                    AccountStatus.NICKNAME_INVALID,
                    "Nickname must be between 3 and 30 characters"
            );
        }
        user.setNickname(nickname);
        this.repository.save();
        return AccountResult.success("Nickname changed", user);
    }
    public AccountResult changeEmail(User user, String email) {
        if (user == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "User is not available");
        }
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(email)) {
            return AccountResult.failure(
                    AccountStatus.EMAIL_UNCHANGED,
                    "New email must be different from the current email"
            );
        }
        if (!this.isValidEmail(email)) {
            return AccountResult.failure(AccountStatus.EMAIL_INVALID, "Email format is invalid");
        }
        user.setEmail(email);
        this.repository.save();
        return AccountResult.success("Email changed", user);
    }
    public AccountResult changePassword(User user, String newPassword, String oldPassword) {
        if (user == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "User is not available");
        }
        String oldPasswordHash = oldPassword == null ? "" : PasswordHasher.hash(oldPassword);
        if (user.getHashedPassword() == null
                || !user.getHashedPassword().equals(oldPasswordHash)) {
            return AccountResult.failure(
                    AccountStatus.OLD_PASSWORD_INCORRECT,
                    "Old password is incorrect"
            );
        }
        String newPasswordHash = newPassword == null ? "" : PasswordHasher.hash(newPassword);
        if (user.getHashedPassword().equals(newPasswordHash)) {
            return AccountResult.failure(
                    AccountStatus.PASSWORD_UNCHANGED,
                    "New password must be different from the current password"
            );
        }
        String passwordError = this.getPasswordError(newPassword);
        if (passwordError != null) {
            return AccountResult.failure(AccountStatus.PASSWORD_INVALID, passwordError);
        }
        user.setHashedPassword(newPasswordHash);
        this.repository.save();
        return AccountResult.success("Password changed", user);
    }
    public AccountResult beginPasswordReset(String username, String email) {
        this.pendingPasswordReset = null;
        this.passwordResetAnswerVerified = false;
        User user = this.repository.findByUsername(username);
        if (user == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "Username was not found");
        }
        if (email == null || !email.equalsIgnoreCase(user.getEmail())) {
            return AccountResult.failure(AccountStatus.CREDENTIALS_INVALID, "Email does not match");
        }
        if (user.getQuestionNum() < 1 || user.getQuestionNum() > SECURITY_QUESTIONS.size()) {
            return AccountResult.failure(
                    AccountStatus.SECURITY_QUESTION_INVALID,
                    "Stored security question is invalid"
            );
        }
        this.pendingPasswordReset = user;
        this.passwordResetAnswerVerified = false;
        String question = SECURITY_QUESTIONS.get(user.getQuestionNum() - 1);
        return AccountResult.question("Answer the security question", question, user);
    }
    public AccountResult verifyPasswordResetAnswer(String answer) {
        if (this.pendingPasswordReset == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "Password reset was not started");
        }
        String hashedAnswer = answer == null
                ? ""
                : PasswordHasher.hash(this.normalizeAnswer(answer));
        if (!this.pendingPasswordReset.getSecurityAnswer().equals(hashedAnswer)) {
            this.pendingPasswordReset = null;
            this.passwordResetAnswerVerified = false;
            return AccountResult.failure(AccountStatus.SECURITY_ANSWER_INVALID, "Security answer is incorrect");
        }
        this.passwordResetAnswerVerified = true;
        return AccountResult.success("Enter a new password", this.pendingPasswordReset);
    }
    public AccountResult completePasswordReset(String newPassword, String newPasswordConfirm) {
        if (this.pendingPasswordReset == null || !this.passwordResetAnswerVerified) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "Security answer is required first");
        }
        String passwordError = this.getPasswordError(newPassword);
        if (passwordError != null) {
            return AccountResult.failure(AccountStatus.PASSWORD_INVALID, passwordError);
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            return AccountResult.failure(AccountStatus.PASSWORD_MISMATCH, "Passwords do not match");
        }
        User user = this.pendingPasswordReset;
        user.setHashedPassword(PasswordHasher.hash(newPassword));
        this.pendingPasswordReset = null;
        this.passwordResetAnswerVerified = false;
        this.repository.save();
        return AccountResult.success("Password changed", user);
    }
    public AccountResult completePasswordReset(
            String answer,
            String newPassword,
            String newPasswordConfirm
    ) {
        AccountResult answerResult = this.verifyPasswordResetAnswer(answer);
        if (!answerResult.isSuccessful()) {
            return answerResult;
        }
        return this.completePasswordReset(newPassword, newPasswordConfirm);
    }
    public User getRememberedUser() {
        return this.repository.getRememberedUser();
    }
    public void logout() {
        User user = this.repository.getRememberedUser();
        if (user != null) {
            user.setStayLoggedIn(false);
        }
        this.repository.remember(null);
    }
    public void save() {
        this.repository.save();
    }
    @Override
    public void close() {
        this.save();
    }
    private void saveProfileChange(User user) {
        if (user.isStayLoggedIn()) {
            this.repository.remember(user);
        } else {
            this.repository.save();
        }
    }
    private Comparator<User> leaderboardComparator(LeaderboardSortField sortField) {
        LeaderboardSortField selected = sortField == null
                ? LeaderboardSortField.MEOW_POINTS
                : sortField;
        return switch (selected) {
            case USERNAME -> Comparator.comparing(
                    User::getUsername,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case PROGRESS -> Comparator.comparingInt(this::progressChapterIndex)
                    .thenComparingInt(this::progressLevel);
            case MINIGAMES -> Comparator.comparingInt(User::getCompletedMinigames);
            case DAILY_QUESTS -> Comparator.comparingInt(User::getCompletedDailyQuests);
            case NON_DAILY_QUESTS -> Comparator.comparingInt(User::getCompletedNonDailyQuests);
            case MEOW_POINTS -> Comparator.comparingInt(User::getMaxObtainedMeowPoints);
        };
    }
    private int progressChapterIndex(User user) {
        if (user == null) {
            return -1;
        }
        if (user.getLastCompletedChapterType() != null) {
            return user.getLastCompletedChapterType().ordinal();
        }
        return user.getChapter() == null || user.getChapter().getChapter() == null
                ? -1
                : user.getChapter().getChapter().ordinal();
    }
    private int progressLevel(User user) {
        if (user == null) {
            return 0;
        }
        if (user.getLastCompletedLevel() > 0) {
            return user.getLastCompletedLevel();
        }
        return user.getChapter() == null ? 0 : Math.max(0, user.getLevel() - 1);
    }
    private boolean isValidUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9-]+");
    }
    private String getPasswordError(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        boolean hasLowercase = false;
        boolean hasUppercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);
            if (Character.isLowerCase(character)) {
                hasLowercase = true;
            } else if (Character.isUpperCase(character)) {
                hasUppercase = true;
            } else if (Character.isDigit(character)) {
                hasDigit = true;
            } else if (SPECIAL_CHARACTERS.indexOf(character) >= 0) {
                hasSpecial = true;
            } else {
                return "Password contains an invalid character";
            }
        }
        if (!hasLowercase) {
            return "Password must contain a lowercase letter";
        }
        if (!hasUppercase) {
            return "Password must contain an uppercase letter";
        }
        if (!hasDigit) {
            return "Password must contain a number";
        }
        if (!hasSpecial) {
            return "Password must contain a special character";
        }
        return null;
    }
    private boolean isValidEmail(String email) {
        if (email == null || email.contains("..")) {
            return false;
        }
        String[] parts = email.split("@", -1);
        if (parts.length != 2 || !this.isValidEmailLocalPart(parts[0])) {
            return false;
        }
        String[] domainParts = parts[1].split("\\.", -1);
        if (domainParts.length < 2) {
            return false;
        }
        for (String domainPart : domainParts) {
            if (!domainPart.matches("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?")) {
                return false;
            }
        }
        return domainParts[domainParts.length - 1].matches("[A-Za-z]{2,}");
    }
    private boolean isValidEmailLocalPart(String localPart) {
        return localPart.matches("[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?");
    }
    private String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase(Locale.ROOT);
    }
    // etelaate movaghate sabt nam ro negah midare
    private static final class PendingRegistration {
        private final String username;
        private final String hashedPassword;
        private final String nickname;
        private final String email;
        private final UserGender gender;
        private PendingRegistration(
                String username,
                String hashedPassword,
                String nickname,
                String email,
                UserGender gender
        ) {
            this.username = username;
            this.hashedPassword = hashedPassword;
            this.nickname = nickname;
            this.email = email;
            this.gender = gender;
        }
    }
}
