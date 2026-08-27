package model.User;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import network.client.GameClient;
import network.client.NetworkException;
import network.protocol.NetworkResponse;
import network.protocol.RequestType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class NetworkAccountService extends AccountService {
    private static final String SPECIAL_CHARACTERS = "?><,\"';:\\/|[]}{+=()*&^%$#!";
    private static final List<String> SECURITY_QUESTIONS = Collections.unmodifiableList(Arrays.asList(
            "What was the name of your first pet?",
            "In which city were you born?",
            "What is your favorite plant?"
    ));

    private final GameClient client;
    private final ClientSessionStore sessionStore;
    private final Gson gson;
    private PendingRegistration pendingRegistration;
    private String pendingPasswordResetUsername;
    private String passwordResetToken;
    private String authToken;
    private User currentUser;

    public NetworkAccountService(GameClient client) {
        this(client, Paths.get(
                System.getProperty("user.home"),
                ".plants-vs-zombies-2",
                "session.json"
        ));
    }

    public NetworkAccountService(GameClient client, Path sessionPath) {
        super();
        if (client == null || sessionPath == null) {
            throw new IllegalArgumentException("Client and session path are required");
        }
        this.client = client;
        this.sessionStore = new ClientSessionStore(sessionPath);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
    }

    @Override
    public List<String> getSecurityQuestions() {
        return SECURITY_QUESTIONS;
    }

    @Override
    public void clearPendingRegistration() {
        this.pendingRegistration = null;
    }

    @Override
    public void clearPendingPasswordReset() {
        this.pendingPasswordResetUsername = null;
        this.passwordResetToken = null;
    }

    @Override
    public AccountResult beginRegistration(
            String username,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            UserGender gender
    ) {
        this.pendingRegistration = null;
        AccountResult validation = validateRegistration(
                username, password, passwordConfirm, nickname, email, gender);
        if (validation != null) {
            return validation;
        }
        this.pendingRegistration = new PendingRegistration(username, password, nickname, email, gender);
        return AccountResult.questionRequired("Pick a security question");
    }

    @Override
    public AccountResult completeRegistration(int questionNumber, String answer, String answerConfirm) {
        if (this.pendingRegistration == null) {
            return AccountResult.failure(
                    AccountStatus.SECURITY_QUESTION_INVALID,
                    "Registration information is required first");
        }
        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.size()) {
            return AccountResult.failure(
                    AccountStatus.SECURITY_QUESTION_INVALID,
                    "Security question number is invalid");
        }
        if (answer == null || answer.trim().isEmpty() || !answer.equals(answerConfirm)) {
            return AccountResult.failure(
                    AccountStatus.SECURITY_ANSWER_INVALID,
                    "Security answers do not match");
        }

        PendingRegistration registration = this.pendingRegistration;
        User user = new User(
                registration.username,
                null,
                registration.nickname,
                registration.email,
                questionNumber,
                null,
                registration.gender);
        JsonObject payload = new JsonObject();
        payload.addProperty("username", registration.username);
        payload.addProperty("password", registration.password);
        payload.addProperty("nickname", registration.nickname);
        payload.addProperty("email", registration.email);
        payload.addProperty("gender", registration.gender.name());
        payload.addProperty("questionNumber", questionNumber);
        payload.addProperty("securityAnswer", answer);
        payload.add("user", this.gson.toJsonTree(user));

        NetworkResponse response = send(RequestType.REGISTER, payload);
        AccountResult failure = failure(response);
        if (failure != null) {
            return failure;
        }
        this.pendingRegistration = null;
        return AccountResult.success(message(response, "Registration completed"), user);
    }

    @Override
    public AccountResult login(String username, String password, boolean stayLoggedIn) {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("password", password);
        payload.addProperty("stayLoggedIn", stayLoggedIn);
        NetworkResponse response = send(RequestType.LOGIN, payload);
        AccountResult failure = failure(response);
        if (failure != null) {
            return failure;
        }
        this.authToken = string(response.getPayload(), "token");
        this.currentUser = readUser(response.getPayload());
        if (this.currentUser == null || this.authToken == null) {
            return AccountResult.failure(AccountStatus.SERVER_UNAVAILABLE, "Server returned invalid account data");
        }
        this.currentUser.setStayLoggedIn(stayLoggedIn);
        if (stayLoggedIn) {
            this.sessionStore.save(this.authToken);
        } else {
            this.sessionStore.clear();
        }
        return AccountResult.success(message(response, "Login successful"), this.currentUser);
    }

    @Override
    public List<User> getUsers() {
        return this.currentUser == null ? Collections.emptyList() : Collections.singletonList(this.currentUser);
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard(
            LeaderboardSortField sortField,
            boolean ascending
    ) {
        save();
        JsonObject payload = authenticatedPayload();
        payload.addProperty("sortField", (sortField == null
                ? LeaderboardSortField.MEOW_POINTS
                : sortField).name());
        payload.addProperty("ascending", ascending);
        NetworkResponse response = send(RequestType.GET_LEADERBOARD, payload);
        if (failure(response) != null
                || !response.getPayload().has("entries")
                || !response.getPayload().get("entries").isJsonArray()) {
            return Collections.emptyList();
        }
        List<LeaderboardEntry> entries = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement value : response.getPayload().getAsJsonArray("entries")) {
            if (value != null && value.isJsonObject()) {
                JsonObject entry = value.getAsJsonObject();
                entries.add(new LeaderboardEntry(
                        integer(entry, "rank"),
                        string(entry, "username"),
                        string(entry, "nickname"),
                        string(entry, "lastChapter"),
                        integer(entry, "lastLevel"),
                        integer(entry, "completedMinigames"),
                        integer(entry, "completedDailyQuests"),
                        integer(entry, "completedNonDailyQuests"),
                        integer(entry, "meowPoints")
                ));
            }
        }
        return entries;
    }

    @Override
    public AccountResult changeUsername(User user, String username) {
        if (user == null || this.authToken == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "User is not available");
        }
        if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(username)) {
            return AccountResult.failure(AccountStatus.USERNAME_UNCHANGED,
                    "New username must be different from the current username");
        }
        JsonObject payload = authenticatedPayload();
        payload.addProperty("field", "username");
        payload.addProperty("value", username);
        AccountResult result = updateProfile(payload, user);
        if (result.isSuccessful()) {
            user.setUsername(username);
        }
        return result;
    }

    @Override
    public AccountResult changeNickname(User user, String nickname) {
        if (user == null || this.authToken == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "User is not available");
        }
        if (user.getNickname() != null && user.getNickname().equals(nickname)) {
            return AccountResult.failure(AccountStatus.NICKNAME_UNCHANGED,
                    "New nickname must be different from the current nickname");
        }
        JsonObject payload = authenticatedPayload();
        payload.addProperty("field", "nickname");
        payload.addProperty("value", nickname);
        AccountResult result = updateProfile(payload, user);
        if (result.isSuccessful()) {
            user.setNickname(nickname);
        }
        return result;
    }

    @Override
    public AccountResult changeEmail(User user, String email) {
        if (user == null || this.authToken == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "User is not available");
        }
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(email)) {
            return AccountResult.failure(AccountStatus.EMAIL_UNCHANGED,
                    "New email must be different from the current email");
        }
        JsonObject payload = authenticatedPayload();
        payload.addProperty("field", "email");
        payload.addProperty("value", email);
        AccountResult result = updateProfile(payload, user);
        if (result.isSuccessful()) {
            user.setEmail(email);
        }
        return result;
    }

    @Override
    public AccountResult changePassword(User user, String newPassword, String oldPassword) {
        if (user == null || this.authToken == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "User is not available");
        }
        JsonObject payload = authenticatedPayload();
        payload.addProperty("field", "password");
        payload.addProperty("oldPassword", oldPassword);
        payload.addProperty("newPassword", newPassword);
        return updateProfile(payload, user);
    }

    @Override
    public AccountResult beginPasswordReset(String username, String email) {
        clearPendingPasswordReset();
        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("email", email);
        NetworkResponse response = send(RequestType.BEGIN_PASSWORD_RESET, payload);
        AccountResult failure = failure(response);
        if (failure != null) {
            return failure;
        }
        int questionNumber = response.getPayload().get("questionNumber").getAsInt();
        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.size()) {
            return AccountResult.failure(AccountStatus.SECURITY_QUESTION_INVALID,
                    "Stored security question is invalid");
        }
        this.pendingPasswordResetUsername = username;
        return AccountResult.question(
                message(response, "Answer the security question"),
                SECURITY_QUESTIONS.get(questionNumber - 1),
                null);
    }

    @Override
    public AccountResult verifyPasswordResetAnswer(String answer) {
        if (this.pendingPasswordResetUsername == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND, "Password reset was not started");
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("username", this.pendingPasswordResetUsername);
        payload.addProperty("securityAnswer", answer);
        NetworkResponse response = send(RequestType.VERIFY_PASSWORD_RESET, payload);
        AccountResult failure = failure(response);
        if (failure != null) {
            clearPendingPasswordReset();
            return failure;
        }
        this.passwordResetToken = string(response.getPayload(), "resetToken");
        return AccountResult.success(message(response, "Enter a new password"), null);
    }

    @Override
    public AccountResult completePasswordReset(String newPassword, String newPasswordConfirm) {
        if (this.passwordResetToken == null) {
            return AccountResult.failure(AccountStatus.USER_NOT_FOUND,
                    "Security answer is required first");
        }
        String passwordError = passwordError(newPassword);
        if (passwordError != null) {
            return AccountResult.failure(AccountStatus.PASSWORD_INVALID, passwordError);
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            return AccountResult.failure(AccountStatus.PASSWORD_MISMATCH, "Passwords do not match");
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("resetToken", this.passwordResetToken);
        payload.addProperty("newPassword", newPassword);
        NetworkResponse response = send(RequestType.COMPLETE_PASSWORD_RESET, payload);
        AccountResult failure = failure(response);
        if (failure != null) {
            return failure;
        }
        clearPendingPasswordReset();
        return AccountResult.success(message(response, "Password changed"), null);
    }

    @Override
    public AccountResult completePasswordReset(
            String answer,
            String newPassword,
            String newPasswordConfirm
    ) {
        AccountResult verification = verifyPasswordResetAnswer(answer);
        return verification.isSuccessful()
                ? completePasswordReset(newPassword, newPasswordConfirm)
                : verification;
    }

    @Override
    public User getRememberedUser() {
        String storedToken = this.sessionStore.load();
        if (storedToken == null) {
            return null;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("token", storedToken);
        NetworkResponse response = send(RequestType.RESUME_SESSION, payload);
        if (failure(response) != null) {
            this.sessionStore.clear();
            return null;
        }
        User restored = readUser(response.getPayload());
        if (restored == null) {
            this.sessionStore.clear();
            return null;
        }
        this.authToken = storedToken;
        this.currentUser = restored;
        this.currentUser.setStayLoggedIn(true);
        return this.currentUser;
    }

    @Override
    public void logout() {
        if (this.authToken != null) {
            send(RequestType.LOGOUT, authenticatedPayload());
        }
        if (this.currentUser != null) {
            this.currentUser.setStayLoggedIn(false);
        }
        this.authToken = null;
        this.currentUser = null;
        this.sessionStore.clear();
    }

    @Override
    public void save() {
        if (this.authToken == null || this.currentUser == null) {
            return;
        }
        this.currentUser.prepareForSave();
        JsonObject payload = authenticatedPayload();
        payload.add("user", this.gson.toJsonTree(this.currentUser));
        send(RequestType.SYNC_ACCOUNT, payload);
    }

    @Override
    public void close() {
        try {
            save();
        } finally {
            this.client.close();
        }
    }

    private AccountResult updateProfile(JsonObject payload, User user) {
        NetworkResponse response = send(RequestType.UPDATE_PROFILE, payload);
        AccountResult failure = failure(response);
        return failure == null
                ? AccountResult.success(message(response, "Profile updated"), user)
                : failure;
    }

    private JsonObject authenticatedPayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("token", this.authToken);
        return payload;
    }

    private NetworkResponse send(RequestType type, JsonObject payload) {
        try {
            return this.client.send(type, payload);
        } catch (NetworkException e) {
            return null;
        }
    }

    private AccountResult failure(NetworkResponse response) {
        if (response == null) {
            return AccountResult.failure(AccountStatus.SERVER_UNAVAILABLE, "Could not reach the server");
        }
        if (!response.isSuccessful()) {
            return AccountResult.failure(AccountStatus.SERVER_UNAVAILABLE, response.getMessage());
        }
        String statusName = string(response.getPayload(), "status");
        if (statusName == null || AccountStatus.SUCCESS.name().equals(statusName)) {
            return null;
        }
        try {
            return AccountResult.failure(AccountStatus.valueOf(statusName), message(response, "Request failed"));
        } catch (IllegalArgumentException e) {
            return AccountResult.failure(AccountStatus.SERVER_UNAVAILABLE, "Server returned an invalid status");
        }
    }

    private User readUser(JsonObject payload) {
        if (payload == null || !payload.has("user") || !payload.get("user").isJsonObject()) {
            return null;
        }
        User user = this.gson.fromJson(payload.getAsJsonObject("user"), User.class);
        if (user != null) {
            user.initializeMissingFields();
        }
        return user;
    }

    private String message(NetworkResponse response, String fallback) {
        String value = response == null ? null : string(response.getPayload(), "message");
        return value == null || value.isBlank() ? fallback : value;
    }

    private String string(JsonObject payload, String name) {
        return payload == null || !payload.has(name) || payload.get(name).isJsonNull()
                ? null
                : payload.get(name).getAsString();
    }

    private int integer(JsonObject payload, String name) {
        return payload == null || !payload.has(name) || payload.get(name).isJsonNull()
                ? 0
                : payload.get(name).getAsInt();
    }

    private AccountResult validateRegistration(
            String username,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            UserGender gender
    ) {
        if (!validUsername(username)) {
            return AccountResult.failure(AccountStatus.USERNAME_INVALID,
                    "Username may only contain letters numbers and hyphens");
        }
        String passwordError = passwordError(password);
        if (passwordError != null) {
            return AccountResult.failure(AccountStatus.PASSWORD_INVALID, passwordError);
        }
        if (!password.equals(passwordConfirm)) {
            return AccountResult.failure(AccountStatus.PASSWORD_MISMATCH, "Passwords do not match");
        }
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            return AccountResult.failure(AccountStatus.NICKNAME_INVALID,
                    "Nickname must be between 3 and 30 characters");
        }
        if (!validEmail(email)) {
            return AccountResult.failure(AccountStatus.EMAIL_INVALID, "Email format is invalid");
        }
        if (gender == null) {
            return AccountResult.failure(AccountStatus.GENDER_INVALID, "Gender must be male or female");
        }
        return null;
    }

    private boolean validUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9-]+");
    }

    private String passwordError(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        boolean lowercase = false;
        boolean uppercase = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);
            if (Character.isLowerCase(character)) {
                lowercase = true;
            } else if (Character.isUpperCase(character)) {
                uppercase = true;
            } else if (Character.isDigit(character)) {
                digit = true;
            } else if (SPECIAL_CHARACTERS.indexOf(character) >= 0) {
                special = true;
            } else {
                return "Password contains an invalid character";
            }
        }
        if (!lowercase) return "Password must contain a lowercase letter";
        if (!uppercase) return "Password must contain an uppercase letter";
        if (!digit) return "Password must contain a number";
        if (!special) return "Password must contain a special character";
        return null;
    }

    private boolean validEmail(String email) {
        if (email == null || email.contains("..")) return false;
        String[] parts = email.split("@", -1);
        if (parts.length != 2
                || !parts[0].matches("[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?")) {
            return false;
        }
        String[] domainParts = parts[1].split("\\.", -1);
        if (domainParts.length < 2) return false;
        for (String part : domainParts) {
            if (!part.matches("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?")) return false;
        }
        return domainParts[domainParts.length - 1].matches("[A-Za-z]{2,}");
    }

    private static final class PendingRegistration {
        private final String username;
        private final String password;
        private final String nickname;
        private final String email;
        private final UserGender gender;

        private PendingRegistration(
                String username,
                String password,
                String nickname,
                String email,
                UserGender gender
        ) {
            this.username = username;
            this.password = password;
            this.nickname = nickname;
            this.email = email;
            this.gender = gender;
        }
    }
}
