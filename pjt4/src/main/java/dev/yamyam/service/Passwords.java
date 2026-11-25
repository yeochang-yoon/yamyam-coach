package dev.yamyam.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {

    private static final int ITERATIONS = 210_000;

    private Passwords() {
    }

    public static String hash(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException("비밀번호는 8~128자여야 합니다.");
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
            + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean matches(String password, String hash) {
        if (password == null || password.length() > 128) {
            return false;
        }
        try {
            String[] parts = hash.split(":");
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[2]),
                derive(password, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0])));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
                .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
