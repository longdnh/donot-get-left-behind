package com.longdnh.donot_get_left_behind.identity.application.port;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.longdnh.donot_get_left_behind.identity.domain.AuthSession;

public interface AuthSessionRepository {

    AuthSession create(AuthSession session);

    Optional<AuthSession> findById(UUID id);

    boolean recordActivityIfActive(
            UUID sessionId,
            UUID userId,
            Instant now,
            Duration idleTimeout);

}
