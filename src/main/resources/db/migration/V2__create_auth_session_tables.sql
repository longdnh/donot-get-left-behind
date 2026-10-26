CREATE TABLE auth_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,

    created_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    absolute_expires_at timestamptz NOT NULL,
    revoked_at timestamptz,

    CONSTRAINT ck_auth_sessions_uuid_v7
        CHECK ((uuid_extract_version(id) = 7) IS TRUE),

    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_auth_sessions_absolute_expiry
        CHECK (absolute_expires_at > created_at),

    CONSTRAINT ck_auth_sessions_activity
        CHECK (
            last_seen_at >= created_at
            AND last_seen_at < absolute_expires_at
        ),

    CONSTRAINT ck_auth_sessions_revocation
        CHECK (
            revoked_at IS NULL
            OR revoked_at >= last_seen_at
        )
);

CREATE INDEX idx_auth_sessions_user
    ON auth_sessions (user_id);