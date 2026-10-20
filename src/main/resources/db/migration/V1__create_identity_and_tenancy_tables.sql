-- PostgreSQL 18+. UUIDv7 IDs and timestamps are supplied by the application.
CREATE TABLE users (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL,
    password_hash text NOT NULL,
    status varchar(32) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_users_uuid_v7 CHECK ((uuid_extract_version(id) = 7) IS TRUE),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_canonical CHECK (
        email = lower(btrim(email)) AND email ~ '[^[:space:]]'
    ),
    CONSTRAINT ck_users_password_hash CHECK (password_hash ~ '[^[:space:]]'),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_users_timestamps CHECK (updated_at >= created_at)
);

CREATE TABLE organizations (
    id uuid PRIMARY KEY,
    name varchar(200) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_organizations_uuid_v7 CHECK ((uuid_extract_version(id) = 7) IS TRUE),
    CONSTRAINT ck_organizations_name CHECK (name = btrim(name) AND name ~ '[^[:space:]]'),
    CONSTRAINT ck_organizations_timestamps CHECK (updated_at >= created_at)
);

CREATE TABLE memberships (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role varchar(32) NOT NULL,
    status varchar(32) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_memberships_uuid_v7 CHECK ((uuid_extract_version(id) = 7) IS TRUE),
    CONSTRAINT fk_memberships_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_memberships_organization_user UNIQUE (organization_id, user_id),
    CONSTRAINT ck_memberships_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT ck_memberships_status CHECK (status IN ('ACTIVE', 'REMOVED')),
    CONSTRAINT ck_memberships_timestamps CHECK (updated_at >= created_at)
);

-- Listing the current user's active organizations.
CREATE INDEX idx_memberships_user_status ON memberships (user_id, status, organization_id);

-- Candidate owners; the service must also check the joined user's ACTIVE status.
CREATE INDEX idx_memberships_active_owners ON memberships (organization_id, user_id)
    WHERE role = 'OWNER' AND status = 'ACTIVE';

-- Retaining a last active owner requires a transaction and organization row lock.
-- A row CHECK constraint cannot enforce this cross-row rule.
