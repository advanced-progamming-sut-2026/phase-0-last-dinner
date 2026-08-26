package network.server;

import com.google.gson.JsonObject;

final class ServerUserRecord {
    String username;
    String passwordSalt;
    String passwordHash;
    String nickname;
    String email;
    int questionNumber;
    String securityAnswerSalt;
    String securityAnswerHash;
    String gender;
    String rememberedTokenHash;
    JsonObject user;
}
