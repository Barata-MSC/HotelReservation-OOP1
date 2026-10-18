package com.hotel.core.service;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2-HMAC-SHA256 password hashing with a unique random salt per hash. */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT = "pbkdf2-sha256";
    private static final int DEFAULT_ITERATIONS = 310_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int MIN_ITERATIONS = 10_000;
    private static final int MAX_ITERATIONS = 2_000_000;
    public static final int MIN_PASSWORD_LENGTH = 10;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final Pattern BASE64_URL = Pattern.compile("[A-Za-z0-9_-]+");

    private final SecureRandom secureRandom = new SecureRandom();
    private final int iterations;

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    PasswordHasher(int iterations) {
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException("PBKDF2 iteration count is outside the permitted range");
        }
        this.iterations = iterations;
    }

    public String hash(char[] password) {
        validatePassword(password);
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(password, salt, iterations);
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return FORMAT + "$" + iterations + "$"
                    + encoder.encodeToString(salt) + "$"
                    + encoder.encodeToString(derived);
        } finally {
            java.util.Arrays.fill(derived, (byte) 0);
        }
    }

    public boolean verify(char[] password, String encodedHash) {
        if (password == null || password.length == 0 || password.length > MAX_PASSWORD_LENGTH
                || encodedHash == null || encodedHash.length() > 256) {
            return false;
        }

        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !FORMAT.equals(parts[0]) || !isPositiveInteger(parts[1])) {
            return false;
        }

        int encodedIterations;
        byte[] salt;
        byte[] expected;
        try {
            encodedIterations = Integer.parseInt(parts[1]);
            if (encodedIterations < MIN_ITERATIONS || encodedIterations > MAX_ITERATIONS
                    || !BASE64_URL.matcher(parts[2]).matches()
                    || !BASE64_URL.matcher(parts[3]).matches()) {
                return false;
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            salt = decoder.decode(parts[2]);
            expected = decoder.decode(parts[3]);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (salt.length != SALT_LENGTH_BYTES || expected.length != KEY_LENGTH_BITS / Byte.SIZE) {
            return false;
        }

        byte[] actual = derive(password, salt, encodedIterations);
        try {
            return java.security.MessageDigest.isEqual(expected, actual);
        } finally {
            java.util.Arrays.fill(actual, (byte) 0);
            java.util.Arrays.fill(expected, (byte) 0);
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int workFactor) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, workFactor, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA256 is not available", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH
                || password.length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be between " + MIN_PASSWORD_LENGTH + " and "
                            + MAX_PASSWORD_LENGTH + " characters");
        }
    }

    private static boolean isPositiveInteger(String value) {
        if (value.isEmpty() || value.length() > 7) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
