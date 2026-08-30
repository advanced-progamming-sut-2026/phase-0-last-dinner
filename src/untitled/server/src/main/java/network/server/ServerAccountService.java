package network.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import network.protocol.RequestType;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerAccountService {
    private static final String SPECIAL_CHARACTERS = "?><,\"';:\\/|[]}{+=()*&^%$#!";

    private final ServerUserRepository repository;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> passwordResets = new ConcurrentHashMap<>();

    public ServerAccountService(ServerUserRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Repository is required");
        }
        this.repository = repository;
    }

    public void registerRoutes(RequestRouter router) {
        router.register(RequestType.REGISTER, this::register);
        router.register(RequestType.LOGIN, this::login);
        router.register(RequestType.RESUME_SESSION, this::resumeSession);
        router.register(RequestType.LOGOUT, this::logout);
        router.register(RequestType.BEGIN_PASSWORD_RESET, this::beginPasswordReset);
        router.register(RequestType.VERIFY_PASSWORD_RESET, this::verifyPasswordReset);
        router.register(RequestType.COMPLETE_PASSWORD_RESET, this::completePasswordReset);
        router.register(RequestType.GET_PROFILE, this::getProfile);
        router.register(RequestType.UPDATE_PROFILE, this::updateProfile);
        router.register(RequestType.SYNC_ACCOUNT, this::syncAccount);
    }

    private synchronized JsonObject register(JsonObject payload) {
        String username = string(payload, "username");
        String password = string(payload, "password");
        String nickname = string(payload, "nickname");
        String email = string(payload, "email");
        String gender = string(payload, "gender");
        int questionNumber = integer(payload, "questionNumber");
        String answer = string(payload, "securityAnswer");

        JsonObject validation = validateRegistration(
                username, password, nickname, email, gender, questionNumber, answer);
        if (validation != null) {
            return validation;
        }
        if (this.repository.findByUsername(username) != null) {
            return failure("USERNAME_TAKEN", "Username already exists");
        }

        ServerCredentialHasher.Credential passwordCredential = ServerCredentialHasher.create(password);
        ServerCredentialHasher.Credential answerCredential =
                ServerCredentialHasher.create(normalizeAnswer(answer));
        ServerUserRecord record = new ServerUserRecord();
        record.username = username;
        record.passwordSalt = passwordCredential.salt;
        record.passwordHash = passwordCredential.hash;
        record.nickname = nickname;
        record.email = email;
        record.questionNumber = questionNumber;
        record.securityAnswerSalt = answerCredential.salt;
        record.securityAnswerHash = answerCredential.hash;
        record.gender = gender.toUpperCase(Locale.ROOT);
        record.user = userObject(payload);
        applyAuthoritativeFields(record);
        if (!this.repository.add(record)) {
            return failure("USERNAME_TAKEN", "Username already exists");
        }
        return success("Registration completed", record, null);
    }

    private synchronized JsonObject login(JsonObject payload) {
        ServerUserRecord record = this.repository.findByUsername(string(payload, "username"));
        if (record == null) {
            return failure("USER_NOT_FOUND", "Username was not found");
        }
        if (!ServerCredentialHasher.matches(
                string(payload, "password"), record.passwordSalt, record.passwordHash)) {
            return failure("CREDENTIALS_INVALID", "Password is incorrect");
        }

        String token = UUID.randomUUID().toString();
        this.sessions.put(token, record.username);
        boolean stayLoggedIn = bool(payload, "stayLoggedIn");
        record.rememberedTokenHash = stayLoggedIn ? ServerTokenHasher.hash(token) : null;
        applyAuthoritativeFields(record);
        this.repository.save();
        return success("Login successful", record, token);
    }

    private synchronized JsonObject resumeSession(JsonObject payload) {
        String token = string(payload, "token");
        ServerUserRecord record = this.repository.findByRememberedToken(token);
        if (record == null) {
            return failure("SESSION_INVALID", "Saved session is no longer valid");
        }
        this.sessions.put(token, record.username);
        return success("Session restored", record, token);
    }

    private synchronized JsonObject logout(JsonObject payload) {
        String token = string(payload, "token");
        ServerUserRecord record = authenticatedUser(token);
        this.sessions.remove(token);
        if (record != null && ServerTokenHasher.hash(token).equals(record.rememberedTokenHash)) {
            record.rememberedTokenHash = null;
            applyAuthoritativeFields(record);
            this.repository.save();
        }
        return result("SUCCESS", "Logout successful");
    }

    private synchronized JsonObject beginPasswordReset(JsonObject payload) {
        ServerUserRecord record = this.repository.findByUsername(string(payload, "username"));
        if (record == null) {
            return failure("USER_NOT_FOUND", "Username was not found");
        }
        String email = string(payload, "email");
        if (email == null || !email.equalsIgnoreCase(record.email)) {
            return failure("CREDENTIALS_INVALID", "Email does not match");
        }
        JsonObject response = result("SUCCESS", "Answer the security question");
        response.addProperty("questionNumber", record.questionNumber);
        return response;
    }

    private synchronized JsonObject verifyPasswordReset(JsonObject payload) {
        ServerUserRecord record = this.repository.findByUsername(string(payload, "username"));
        if (record == null) {
            return failure("USER_NOT_FOUND", "Password reset was not started");
        }
        String answer = string(payload, "securityAnswer");
        if (!ServerCredentialHasher.matches(
                answer == null ? null : normalizeAnswer(answer),
                record.securityAnswerSalt,
                record.securityAnswerHash)) {
            return failure("SECURITY_ANSWER_INVALID", "Security answer is incorrect");
        }
        String resetToken = UUID.randomUUID().toString();
        this.passwordResets.put(resetToken, record.username);
        JsonObject response = result("SUCCESS", "Enter a new password");
        response.addProperty("resetToken", resetToken);
        return response;
    }

    private synchronized JsonObject completePasswordReset(JsonObject payload) {
        String resetToken = string(payload, "resetToken");
        String username = this.passwordResets.remove(resetToken);
        ServerUserRecord record = this.repository.findByUsername(username);
        if (record == null) {
            return failure("SESSION_INVALID", "Password reset session is invalid");
        }
        String newPassword = string(payload, "newPassword");
        String passwordError = passwordError(newPassword);
        if (passwordError != null) {
            return failure("PASSWORD_INVALID", passwordError);
        }
        ServerCredentialHasher.Credential credential = ServerCredentialHasher.create(newPassword);
        record.passwordSalt = credential.salt;
        record.passwordHash = credential.hash;
        this.repository.save();
        return result("SUCCESS", "Password changed");
    }

    private synchronized JsonObject getProfile(JsonObject payload) {
        ServerUserRecord record = authenticatedUser(string(payload, "token"));
        return record == null
                ? failure("SESSION_INVALID", "Login is required")
                : success("Profile loaded", record, null);
    }

    private synchronized JsonObject updateProfile(JsonObject payload) {
        String token = string(payload, "token");
        ServerUserRecord record = authenticatedUser(token);
        if (record == null) {
            return failure("SESSION_INVALID", "Login is required");
        }
        String field = string(payload, "field");
        if (field == null) {
            return failure("SERVER_UNAVAILABLE", "Profile field is required");
        }
        return switch (field) {
            case "username" -> changeUsername(record, token, string(payload, "value"));
            case "nickname" -> changeNickname(record, string(payload, "value"));
            case "email" -> changeEmail(record, string(payload, "value"));
            case "password" -> changePassword(
                    record, string(payload, "oldPassword"), string(payload, "newPassword"));
            default -> failure("SERVER_UNAVAILABLE", "Profile field is invalid");
        };
    }

    private JsonObject changeUsername(ServerUserRecord record, String token, String username) {
        if (record.username.equalsIgnoreCase(username)) {
            return failure("USERNAME_UNCHANGED", "New username must be different from the current username");
        }
        if (!validUsername(username)) {
            return failure("USERNAME_INVALID", "Username may only contain letters numbers and hyphens");
        }
        if (!this.repository.rename(record, username)) {
            return failure("USERNAME_TAKEN", "Username already exists");
        }
        this.sessions.put(token, username);
        applyAuthoritativeFields(record);
        this.repository.save();
        return success("Username changed", record, null);
    }

    private JsonObject changeNickname(ServerUserRecord record, String nickname) {
        if (record.nickname != null && record.nickname.equals(nickname)) {
            return failure("NICKNAME_UNCHANGED", "New nickname must be different from the current nickname");
        }
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            return failure("NICKNAME_INVALID", "Nickname must be between 3 and 30 characters");
        }
        record.nickname = nickname;
        applyAuthoritativeFields(record);
        this.repository.save();
        return success("Nickname changed", record, null);
    }

    private JsonObject changeEmail(ServerUserRecord record, String email) {
        if (record.email != null && record.email.equalsIgnoreCase(email)) {
            return failure("EMAIL_UNCHANGED", "New email must be different from the current email");
        }
        if (!validEmail(email)) {
            return failure("EMAIL_INVALID", "Email format is invalid");
        }
        record.email = email;
        applyAuthoritativeFields(record);
        this.repository.save();
        return success("Email changed", record, null);
    }

    private JsonObject changePassword(ServerUserRecord record, String oldPassword, String newPassword) {
        if (!ServerCredentialHasher.matches(oldPassword, record.passwordSalt, record.passwordHash)) {
            return failure("OLD_PASSWORD_INCORRECT", "Old password is incorrect");
        }
        if (ServerCredentialHasher.matches(newPassword, record.passwordSalt, record.passwordHash)) {
            return failure("PASSWORD_UNCHANGED", "New password must be different from the current password");
        }
        String passwordError = passwordError(newPassword);
        if (passwordError != null) {
            return failure("PASSWORD_INVALID", passwordError);
        }
        ServerCredentialHasher.Credential credential = ServerCredentialHasher.create(newPassword);
        record.passwordSalt = credential.salt;
        record.passwordHash = credential.hash;
        this.repository.save();
        return success("Password changed", record, null);
    }

    private synchronized JsonObject syncAccount(JsonObject payload) {
        ServerUserRecord record = authenticatedUser(string(payload, "token"));
        if (record == null) {
            return failure("SESSION_INVALID", "Login is required");
        }
        record.user = userObject(payload);
        applyAuthoritativeFields(record);
        this.repository.save();
        return result("SUCCESS", "Account synchronized");
    }

    ServerUserRecord authenticatedUser(String token) {
        String username = token == null ? null : this.sessions.get(token);
        if (username != null) {
            return this.repository.findByUsername(username);
        }
        ServerUserRecord remembered = this.repository.findByRememberedToken(token);
        if (remembered != null) {
            this.sessions.put(token, remembered.username);
        }
        return remembered;
    }

    String authenticatedUsername(String token) {
        ServerUserRecord record = authenticatedUser(token);

        if (record == null)
            return null;

        return record.username;
    }

    private void applyAuthoritativeFields(ServerUserRecord record) {
        if (record.user == null) {
            record.user = new JsonObject();
        }
        record.user.addProperty("username", record.username);
        record.user.addProperty("nickname", record.nickname);
        record.user.addProperty("email", record.email);
        record.user.addProperty("questionNum", record.questionNumber);
        record.user.addProperty("gender", record.gender);
        record.user.addProperty("stayLoggedIn", record.rememberedTokenHash != null);
        record.user.remove("hashedPassword");
        record.user.remove("securityAnswer");
    }

    private JsonObject validateRegistration(
            String username,
            String password,
            String nickname,
            String email,
            String gender,
            int questionNumber,
            String answer
    ) {
        if (!validUsername(username)) {
            return failure("USERNAME_INVALID", "Username may only contain letters numbers and hyphens");
        }
        String passwordError = passwordError(password);
        if (passwordError != null) {
            return failure("PASSWORD_INVALID", passwordError);
        }
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            return failure("NICKNAME_INVALID", "Nickname must be between 3 and 30 characters");
        }
        if (!validEmail(email)) {
            return failure("EMAIL_INVALID", "Email format is invalid");
        }
        if (!"MALE".equalsIgnoreCase(gender) && !"FEMALE".equalsIgnoreCase(gender)) {
            return failure("GENDER_INVALID", "Gender must be male or female");
        }
        if (questionNumber < 1 || questionNumber > 3) {
            return failure("SECURITY_QUESTION_INVALID", "Security question number is invalid");
        }
        if (answer == null || answer.trim().isEmpty()) {
            return failure("SECURITY_ANSWER_INVALID", "Security answer is required");
        }
        return null;
    }

    private JsonObject success(String message, ServerUserRecord record, String token) {
        JsonObject response = result("SUCCESS", message);
        response.add("user", record.user.deepCopy());
        if (token != null) {
            response.addProperty("token", token);
        }
        return response;
    }

    private JsonObject failure(String status, String message) {
        return result(status, message);
    }

    private JsonObject result(String status, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", status);
        response.addProperty("message", message);
        return response;
    }

    private JsonObject userObject(JsonObject payload) {
        JsonElement value = payload == null ? null : payload.get("user");
        return value != null && value.isJsonObject() ? value.getAsJsonObject().deepCopy() : new JsonObject();
    }

    private String string(JsonObject payload, String name) {
        JsonElement value = payload == null ? null : payload.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private int integer(JsonObject payload, String name) {
        JsonElement value = payload == null ? null : payload.get(name);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    private boolean bool(JsonObject payload, String name) {
        JsonElement value = payload == null ? null : payload.get(name);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    private boolean validUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9-]+");
    }

    private String passwordError(String password) {
        if (password == null || password.length() < 8) return "Password must be at least 8 characters";
        boolean lowercase = false;
        boolean uppercase = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);
            if (Character.isLowerCase(character)) lowercase = true;
            else if (Character.isUpperCase(character)) uppercase = true;
            else if (Character.isDigit(character)) digit = true;
            else if (SPECIAL_CHARACTERS.indexOf(character) >= 0) special = true;
            else return "Password contains an invalid character";
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
                || !parts[0].matches("[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?")) return false;
        String[] domainParts = parts[1].split("\\.", -1);
        if (domainParts.length < 2) return false;
        for (String part : domainParts) {
            if (!part.matches("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?")) return false;
        }
        return domainParts[domainParts.length - 1].matches("[A-Za-z]{2,}");
    }

    private String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase(Locale.ROOT);
    }
}
