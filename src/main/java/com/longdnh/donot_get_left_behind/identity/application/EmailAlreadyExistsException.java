package com.longdnh.donot_get_left_behind.identity.application;

public final class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("An account with this email already exists");
    }
}
