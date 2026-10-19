package rw.auca.ac.pharmacy.application;

import rw.auca.ac.pharmacy.application.port.UserRepository;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

public final class AuthService {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;

    public AuthService(UserRepository users) { this.users = users; }

    public boolean authenticate(String username, char[] password) {
        if (username == null || username.isBlank() || password == null || password.length == 0) return false;
        Optional<String> stored = users.findPassword(username.trim());
        if (stored.isEmpty()) return false;
        String value = stored.get();
        if (value.startsWith("pbkdf2$")) return verify(password, value);
        boolean valid = MessageDigest.isEqual(utf8(password), value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (valid) users.updatePassword(username.trim(), encode(password));
        return valid;
    }

    private static String encode(char[] password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verify(char[] password, String encoded) {
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4) return false;
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < 50_000 || rounds > 1_000_000) return false;
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, Base64.getDecoder().decode(parts[2]), rounds);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, rounds, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Password verification is unavailable.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] utf8(char[] value) {
        java.nio.ByteBuffer encoded = java.nio.charset.StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(value));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    }
}
