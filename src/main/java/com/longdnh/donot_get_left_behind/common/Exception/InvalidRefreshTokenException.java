package com.longdnh.donot_get_left_behind.common.Exception;

import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super(ClientMessages.INVALID_REFRESH_TOKEN);
    }
}