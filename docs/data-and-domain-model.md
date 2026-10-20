# Data and domain models

This is the model foundation for the generic multi-tenant authentication backend described in [the authentication design](authentication-authorization.md). There are three concepts: a global user, an organization representing a tenant, and membership connecting the two.

Domain classes, JPA mappings, and the initial Flyway migration are defined. Authorization services, repositories, HTTP endpoints, and UUID generation are separate implementation steps.

## 1. Domain model versus data model

| Layer | Purpose | Examples |
| --- | --- | --- |
| Domain | Express valid state and state transitions | `User`, `Organization`, `Membership`, `Role` |
| Persistence mapping | Translate domain state to database columns | `UserEntity`, `OrganizationEntity`, `MembershipEntity` |
| Database | Store records and enforce structural integrity | `users`, `organizations`, `memberships` |

Domain classes use no Spring or JPA annotations. They are immutable and use Lombok only to generate getters. An operation such as `rename` or `changeRole` returns a new object, which the application service must save.

JPA classes have mutable persistence fields and protected no-argument constructors, as required by [Jakarta Persistence](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/Entity.html). They map through `fromDomain(...)` and `toDomain()`; they are not HTTP response DTOs.

```mermaid
erDiagram
    USERS ||--o{ MEMBERSHIPS : has
    ORGANIZATIONS ||--o{ MEMBERSHIPS : contains
    USERS {
        uuid id PK
        varchar(254) email UK
        text password_hash
        varchar(32) status
        timestamptz created_at
        timestamptz updated_at
    }
    ORGANIZATIONS {
        uuid id PK
        varchar(200) name
        timestamptz created_at
        timestamptz updated_at
    }
    MEMBERSHIPS {
        uuid id PK
        uuid organization_id FK
        uuid user_id FK
        varchar(32) role
        varchar(32) status
        timestamptz created_at
        timestamptz updated_at
    }
```

The diagram expresses database relationships. The service must create an organization and its first owner together; the foreign keys alone allow an organization with zero memberships.

## 2. Domain responsibilities

### User — global identity

`identity.domain.User` contains `id`, `email`, `passwordHash`, `UserStatus`, `createdAt`, and `updatedAt`.

- `User.create(id, email, passwordHash, now)` creates an active account.
- Email is stripped and lowercased using `Locale.ROOT`, preserving dots and `+` suffixes. The canonical value is limited to 254 Unicode code points.
- The password argument is already encoded. Hashing and email-format validation occur before model creation.
- `isActive()` distinguishes `ACTIVE` from `DISABLED`.
- Organization roles and membership lists are not part of this identity model.

### Organization — tenant

`tenancy.domain.Organization` contains `id`, `name`, `createdAt`, and `updatedAt`.

- `create(id, name, now)` creates the organization state.
- `rename(name, changedAt)` returns renamed state with the same ID and creation time.
- Names are stripped, cannot be blank, and are limited to 200 Unicode code points. Different organizations may use the same display name.
- Owner bootstrap and permission to rename require an application service transaction.

### Membership — user access within a tenant

`tenancy.domain.Membership` contains `id`, `organizationId`, `userId`, `Role`, `MembershipStatus`, `createdAt`, and `updatedAt`.

Roles are `OWNER`, `ADMIN`, and `MEMBER`. Status is `ACTIVE` or `REMOVED`.

| Operation | Behavior |
| --- | --- |
| `create(id, organizationId, userId, role, now)` | Creates active membership; `OWNER` is supported for organization bootstrap |
| `changeRole(role, changedAt)` | Changes an active membership's role; removed memberships cannot change role |
| `remove(changedAt)` | Marks membership removed; repeat removal returns existing removed state |
| `reactivate(role, changedAt)` | Restores removed membership with the explicitly assigned role; rejects an already-active membership |

Every transition preserves membership ID, organization ID, user ID, and creation time. Changes cannot move `updatedAt` backward. Rejoining updates the existing membership instead of inserting another row for the same user/organization pair.

These operations enforce local state rules. They do not establish the caller's permission: an application service must check tenant membership, the role matrix, target role restrictions, and the last-owner rule before invoking them.

All three entities compare equality by ID. Comparing two objects with `equals` does not compare their current state.

## 3. PostgreSQL data model

The schema is created by [V1__create_identity_and_tenancy_tables.sql](../src/main/resources/db/migration/V1__create_identity_and_tenancy_tables.sql), including the agreed field limits. It targets PostgreSQL 18+, matching the Compose image.

| Table | Important constraints |
| --- | --- |
| `users` | UUIDv7 primary key; unique canonical email; nonblank email/hash; valid account status; required timestamps in order |
| `organizations` | UUIDv7 primary key; trimmed nonblank name; required timestamps in order |
| `memberships` | UUIDv7 primary key; foreign keys to user and organization; unique `(organization_id, user_id)`; valid role/status; required timestamps in order |

All columns are required. Email uses `varchar(254)`, organization name uses `varchar(200)`, and password hashes use `text`. Roles and statuses use `varchar(32)` with their existing allowed-value check constraints. JPA stores enum names using `EnumType.STRING`, so Java ordinal positions are never persisted.

The email and name limits are application policies enforced after normalization in Java and by PostgreSQL column lengths. Java counts Unicode code points to match the database's character limits. The email length limit does not replace format validation or establish full SMTP/internationalized-email compliance.

Foreign keys use `ON DELETE RESTRICT`. Removing a membership changes its status, preserving the relationship row. The unique pair applies to active and removed memberships alike.

The unique organization/user index supports membership lookup within a tenant. An additional index starts with `user_id` for listing a user's organizations. A partial index covers active owner memberships; the owner check must also join the user's active status.

### IDs and timestamps

All IDs are UUIDv7, supplied to the domain constructors by the application. The JPA mappings deliberately use assigned IDs. There is no `UUID.randomUUID()` or automatic JPA UUID strategy.

PostgreSQL 18 provides `uuidv7()` and `uuid_extract_version()`. The migration uses `CHECK ((uuid_extract_version(id) = 7) IS TRUE)` so UUIDs of another variant, for which extraction returns null, are also rejected. UUID generation remains an infrastructure responsibility; it is not implemented by these models. [PostgreSQL UUID functions](https://www.postgresql.org/docs/18/functions-uuid.html)

Use Java `Instant` and PostgreSQL `timestamptz`. PostgreSQL stores instants with microsecond precision and renders them using the connection's time zone; it does not preserve the original zone name. Use UTC at application boundaries, and account for precision when comparing persisted timestamps. The explicit `createdAt` field remains the creation timestamp; do not substitute the UUID timestamp. [PostgreSQL timestamp types](https://www.postgresql.org/docs/18/datatype-datetime.html)

## 4. Module ownership and persistence mapping

```text
identity/
  domain/User.java
  infrastructure/persistence/UserEntity.java

tenancy/
  domain/Organization.java
  domain/Membership.java
  domain/Role.java
  infrastructure/persistence/OrganizationEntity.java
  infrastructure/persistence/MembershipEntity.java
```

Identity owns `users`. Tenancy owns `organizations` and `memberships`. Membership stores `userId` as a UUID, with no JPA association to `UserEntity`. Tenant references also stay as IDs, avoiding implicit loading of entire object graphs. Database foreign keys still protect referential integrity in the monolith.

Each persistence class maps only its own domain class. Application services should depend on repository interfaces that return domain objects; persistence adapters will use the JPA mappings internally. Request and response DTOs are another boundary, especially because both user models contain a password hash.

## 5. Rules that require application services

The following cannot be guaranteed by a single domain object or ordinary row check constraint:

1. The caller is allowed to access the requested organization.
2. A target user is active before membership creation/reactivation.
3. An admin can add/remove ordinary members but cannot manage owners or other admins.
4. An organization always retains an active owner whose global user is also active.
5. Creating an organization also creates its first owner atomically.

PostgreSQL check constraints cannot safely enforce conditions depending on other rows. The last-owner rule needs a transaction that locks the organization row, reloads the actor and target, and checks active owners before writing. All membership mutations must follow that lock protocol. A future global account-disable operation must use compatible locks across affected organizations. [PostgreSQL constraint rules](https://www.postgresql.org/docs/18/ddl-constraints.html)

Every tenant query must include the requested organization ID, including lookups by membership ID and list/count queries. Foreign keys and UUIDs do not authorize a request. Row-level security is not configured by this migration.

## 6. Validation

Run the domain and mapping tests without starting the Spring application:

```bash
./gradlew test --tests '*UserTest' --tests '*TenancyModelTest' --tests '*ModelMappingTest'
```

The mapping tests check Java conversions, including preservation of hashes, roles, removed status, tenant references, and timestamps. They do not start Hibernate or prove end-to-end repository behavior.

[model_constraints.sql](../src/test/resources/db/model_constraints.sql) checks the schema against a disposable PostgreSQL 18+ database. Apply V1 first in a transaction (for example, `psql -v ON_ERROR_STOP=1 --single-transaction -f V1__create_identity_and_tenancy_tables.sql`), then run the checks with `psql -v ON_ERROR_STOP=1`. It covers field-length boundaries, UUID versions/variants, uniqueness, foreign keys, invalid statuses, timestamp ordering, and reusing a removed membership. Its fixture transaction is rolled back.

Application-level authorization and transaction/concurrency integration tests will be needed when the service layer is implemented.
