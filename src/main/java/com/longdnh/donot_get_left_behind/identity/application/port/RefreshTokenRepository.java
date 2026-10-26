package com.longdnh.donot_get_left_behind.identity.application.port;

import com.longdnh.donot_get_left_behind.identity.domain.RefreshToken;

public interface RefreshTokenRepository {

    RefreshToken create(RefreshToken refreshToken);
    
}
