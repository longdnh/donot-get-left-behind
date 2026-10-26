package com.longdnh.donot_get_left_behind.identity.application.exception;

import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

public final class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super(ClientMessages.EMAIL_ALREADY_EXISTS);
    }
}
