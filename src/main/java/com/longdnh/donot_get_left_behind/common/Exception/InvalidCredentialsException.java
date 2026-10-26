package com.longdnh.donot_get_left_behind.common.Exception;

import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

public final class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super(ClientMessages.INVALID_CREDENTIALS);
    }
}
