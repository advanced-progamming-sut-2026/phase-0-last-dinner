package network.server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

final class ServerCredentialHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ServerCredentialHasher() {
    }

    static Credential create(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value is required");
        }
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return new Credential(
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(derive(value, salt))
        );
    }

    static boolean matches(String value, String saltValue, String hashValue) {
        if (value == null || saltValue == null || hashValue == null) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(saltValue);
        byte[] expected = Base64.getDecoder().decode(hashValue);
        return MessageDigest.isEqual(expected, derive(value, salt));
    }

    private static byte[] derive(String value, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(value.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    static final class Credential {
        final String salt;
        final String hash;

        Credential(String salt, String hash) {
            this.salt = salt;
            this.hash = hash;
        }
    }
}
