package com.hotel.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher(10_000);

    @Test
    void hashesAreSaltedAndCanBeVerified() {
        char[] password = "correct horse battery".toCharArray();
        String firstHash = passwordHasher.hash(password);
        String secondHash = passwordHasher.hash(password);

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordHasher.verify(password, firstHash));
        assertTrue(passwordHasher.verify(password, secondHash));
        assertFalse(passwordHasher.verify("incorrect password".toCharArray(), firstHash));
        assertFalse(firstHash.contains(new String(password)));
    }

    @Test
    void rejectsMalformedHashesAndInvalidNewPasswords() {
        assertFalse(passwordHasher.verify("any password".toCharArray(), "not-a-hash"));
        assertThrows(IllegalArgumentException.class,
                () -> passwordHasher.hash("short".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordHasher(1_000));
    }

    @Test
    void acceptsTenCharacterPasswordsAndRejectsNineCharacters() {
        char[] password = "1234567890".toCharArray();
        String hash = passwordHasher.hash(password);

        assertTrue(passwordHasher.verify(password, hash));
        assertThrows(IllegalArgumentException.class,
                () -> passwordHasher.hash("123456789".toCharArray()));
    }
}
