package com.hotel.core.exception;

public final class InvalidCredentialsException extends RuntimeException {

    public static final String GENERIC_MESSAGE = "Invalid username/email or password.";

    public InvalidCredentialsException() {
        super(GENERIC_MESSAGE);
    }
}
