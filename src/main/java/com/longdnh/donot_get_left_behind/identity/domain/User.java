package com.longdnh.donot_get_left_behind.identity.domain;

import static com.longdnh.donot_get_left_behind.common.Validation.requireNonBlank;
import static com.longdnh.donot_get_left_behind.common.Validation.requireUuidV7;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

@Getter
public final class User {

    public static final int MAX_EMAIL_LENGTH = 254;

    public enum UserStatus {
        ACTIVE,
        DISABLED
    }

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Reconstitutes a user from existing state. The password must already be hashed.
     */
    public User(UUID id, String email, String passwordHash, UserStatus status,
                Instant createdAt, Instant updatedAt) {
        this.id = requireUuidV7(id, "id");
        this.email = requireNonBlank(email, "email").strip().toLowerCase(Locale.ROOT);
        if (this.email.codePointCount(0, this.email.length()) > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("email must not exceed " + MAX_EMAIL_LENGTH + " characters");
        }
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
    }

    public static User create(UUID id, String email, String passwordHash, Instant now) {
        return new User(id, email, passwordHash, UserStatus.ACTIVE, now, now);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof User user && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", status=" + status + "}";
    }
}
