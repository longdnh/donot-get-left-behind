CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY,
    session_id uuid NOT NULL,
    token_hash varchar(64) NOT NULL,

    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    replaced_by_token_id uuid,

    CONSTRAINT ck_refresh_tokens_uuid_v7
        CHECK ((uuid_extract_version(id) = 7) IS TRUE),

    CONSTRAINT fk_refresh_tokens_session
        FOREIGN KEY (session_id)
        REFERENCES auth_sessions (id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_refresh_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT uq_refresh_tokens_session_id
        UNIQUE (session_id, id),

    CONSTRAINT fk_refresh_tokens_replacement
        FOREIGN KEY (session_id, replaced_by_token_id)
        REFERENCES refresh_tokens (session_id, id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_refresh_tokens_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_refresh_tokens_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT ck_refresh_tokens_consumption_pair
        CHECK (
            (consumed_at IS NULL)
            = (replaced_by_token_id IS NULL)
        ),

    CONSTRAINT ck_refresh_tokens_consumption_time
        CHECK (
            consumed_at IS NULL
            OR (
                consumed_at >= created_at
                AND consumed_at < expires_at
            )
        ),

    CONSTRAINT ck_refresh_tokens_not_self_replacement
        CHECK (
            replaced_by_token_id IS NULL
            OR replaced_by_token_id <> id
        )
);