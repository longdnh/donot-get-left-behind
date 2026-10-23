package com.longdnh.donot_get_left_behind.identity.application.port;

import java.util.Optional;
import java.util.UUID;

import com.longdnh.donot_get_left_behind.identity.domain.User;

public interface UserRepository {

    Optional<User> findById(UUID id);

    /** The application supplies an email stripped and lowercased with Locale.ROOT. */
    Optional<User> findByEmail(String canonicalEmail);

    /** Inserts a new account; throws EmailAlreadyExistsException for a duplicate email. */
    User create(User user);

    /** Saves domain state; database constraints are enforced when the transaction flushes or commits. */
    User save(User user);
}
