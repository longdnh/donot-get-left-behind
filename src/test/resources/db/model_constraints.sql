-- Run against a disposable PostgreSQL 18+ database after applying all migrations.
-- All fixture rows and test helpers are rolled back.
BEGIN;

CREATE FUNCTION pg_temp.expect_sqlstate(statement text, expected_state text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = expected_state THEN
            RETURN;
        END IF;
        RAISE;
    END;
    RAISE EXCEPTION 'Expected SQLSTATE %, but statement succeeded: %', expected_state, statement;
END;
$$;

INSERT INTO users (id, email, password_hash, status, created_at, updated_at) VALUES
    ('019a0000-0000-7000-8000-000000000001', 'alice@example.test', 'test-hash', 'ACTIVE', now(), now()),
    ('019a0000-0000-7000-8000-000000000002', 'bob@example.test', 'test-hash', 'ACTIVE', now(), now());
INSERT INTO organizations (id, name, created_at, updated_at) VALUES
    ('019a0000-0000-7000-8000-000000000003', 'Same name is allowed', now(), now()),
    ('019a0000-0000-7000-8000-000000000004', 'Same name is allowed', now(), now());
INSERT INTO memberships (id, organization_id, user_id, role, status, created_at, updated_at) VALUES
    ('019a0000-0000-7000-8000-000000000005', '019a0000-0000-7000-8000-000000000003',
     '019a0000-0000-7000-8000-000000000001', 'OWNER', 'ACTIVE', now(), now()),
    ('019a0000-0000-7000-8000-000000000006', '019a0000-0000-7000-8000-000000000004',
     '019a0000-0000-7000-8000-000000000001', 'MEMBER', 'ACTIVE', now(), now());

-- Exact character limits are accepted, including non-BMP Unicode characters.
INSERT INTO users VALUES (uuidv7(), repeat('a', 241) || '@example.test', 'hash', 'ACTIVE', now(), now());
INSERT INTO organizations VALUES (uuidv7(), repeat('a', 200), now(), now());
INSERT INTO organizations VALUES (uuidv7(), repeat(chr(128512), 200), now(), now());
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES (uuidv7(), repeat('a', 242) || '@example.test', 'hash', 'ACTIVE', now(), now())
$q$, '22001');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO organizations VALUES (uuidv7(), repeat('a', 201), now(), now())
$q$, '22001');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO organizations VALUES (uuidv7(), repeat(chr(128512), 201), now(), now())
$q$, '22001');

SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES (uuidv7(), 'alice@example.test', 'hash', 'ACTIVE', now(), now())
$q$, '23505');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES (uuidv7(), 'Alice@Example.test', 'hash', 'ACTIVE', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES (uuidv7(), '   ', 'hash', 'ACTIVE', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES (uuidv7(), 'empty@example.test', '   ', 'ACTIVE', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES (uuidv7(), 'null@example.test', NULL, 'ACTIVE', now(), now())
$q$, '23502');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES (gen_random_uuid(), 'v4@example.test', 'hash', 'ACTIVE', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES ('019a0000-0000-7000-0000-000000000007', 'variant@example.test', 'hash', 'ACTIVE', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO users VALUES (uuidv7(), 'status@example.test', 'hash', 'UNKNOWN', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO organizations VALUES (uuidv7(), '   ', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO organizations VALUES (gen_random_uuid(), 'Wrong ID', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO organizations VALUES (uuidv7(), 'Wrong time', now(), now() - interval '1 second')
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO memberships VALUES (uuidv7(), uuidv7(), '019a0000-0000-7000-8000-000000000001',
        'MEMBER', 'ACTIVE', now(), now())
$q$, '23503');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO memberships VALUES (uuidv7(), '019a0000-0000-7000-8000-000000000003', uuidv7(),
        'MEMBER', 'ACTIVE', now(), now())
$q$, '23503');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO memberships VALUES (uuidv7(), '019a0000-0000-7000-8000-000000000003',
        '019a0000-0000-7000-8000-000000000001', 'ADMIN', 'ACTIVE', now(), now())
$q$, '23505');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO memberships VALUES (uuidv7(), '019a0000-0000-7000-8000-000000000003',
        '019a0000-0000-7000-8000-000000000002', 'SUPERUSER', 'ACTIVE', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO memberships VALUES (uuidv7(), '019a0000-0000-7000-8000-000000000003',
        '019a0000-0000-7000-8000-000000000002', 'MEMBER', 'INVITED', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO memberships VALUES (gen_random_uuid(), '019a0000-0000-7000-8000-000000000003',
        '019a0000-0000-7000-8000-000000000002', 'MEMBER', 'ACTIVE', now(), now())
$q$, '23514');
SELECT pg_temp.expect_sqlstate($q$
    DELETE FROM users WHERE id = '019a0000-0000-7000-8000-000000000001'
$q$, '23503');
SELECT pg_temp.expect_sqlstate($q$
    DELETE FROM organizations WHERE id = '019a0000-0000-7000-8000-000000000003'
$q$, '23503');

-- A removed membership still occupies its unique organization/user pair.
UPDATE memberships SET status = 'REMOVED'
WHERE id = '019a0000-0000-7000-8000-000000000006';
SELECT pg_temp.expect_sqlstate($q$
    INSERT INTO memberships VALUES (uuidv7(), '019a0000-0000-7000-8000-000000000004',
        '019a0000-0000-7000-8000-000000000001', 'ADMIN', 'ACTIVE', now(), now())
$q$, '23505');
UPDATE memberships SET status = 'ACTIVE', role = 'ADMIN', updated_at = now() + interval '1 second'
WHERE id = '019a0000-0000-7000-8000-000000000006';

DO $$
BEGIN
    IF (SELECT count(*) FROM memberships
        WHERE user_id = '019a0000-0000-7000-8000-000000000001' AND status = 'ACTIVE') <> 2 THEN
        RAISE EXCEPTION 'User must retain separate memberships in both organizations';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM memberships
        WHERE id = '019a0000-0000-7000-8000-000000000006' AND role = 'ADMIN' AND status = 'ACTIVE') THEN
        RAISE EXCEPTION 'Reactivation must preserve the membership ID and apply the new role';
    END IF;
END;
$$;

ROLLBACK;
