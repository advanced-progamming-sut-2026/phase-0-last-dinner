package network.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class ServerTokenHasher {
    private ServerTokenHasher() {
    }

    static String hash(String token) {
        if (token == null) {
            return null;
        }
        try {
            byte[] encoded = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(encoded.length * 2);
            for (byte item : encoded) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
