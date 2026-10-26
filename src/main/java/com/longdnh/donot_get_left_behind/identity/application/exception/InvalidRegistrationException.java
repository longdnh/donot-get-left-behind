package com.longdnh.donot_get_left_behind.identity.application.exception;

/** Invalid client input, distinct from a programming error or invalid persisted state. */
public final class InvalidRegistrationException extends IllegalArgumentException {

    public InvalidRegistrationException(String message) {
        super(message);
    }
}
