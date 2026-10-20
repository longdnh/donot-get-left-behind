package com.longdnh.donot_get_left_behind.tenancy.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

@Getter
public final class Organization {

    public static final int MAX_NAME_LENGTH = 200;

    private final UUID id;
    private final String name;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Organization(UUID id, String name, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (id.version() != 7 || id.variant() != 2) {
            throw new IllegalArgumentException("id must be an RFC 9562 UUIDv7");
        }
        this.name = Objects.requireNonNull(name, "name is required").strip();
        if (this.name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (this.name.codePointCount(0, this.name.length()) > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
    }

    public static Organization create(UUID id, String name, Instant now) {
        return new Organization(id, name, now, now);
    }

    /** Returns new state; the application service must authorize the caller first. */
    public Organization rename(String name, Instant changedAt) {
        Objects.requireNonNull(changedAt, "changedAt is required");
        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("changedAt must not precede updatedAt");
        }
        return new Organization(id, name, createdAt, changedAt);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Organization organization && id.equals(organization.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
