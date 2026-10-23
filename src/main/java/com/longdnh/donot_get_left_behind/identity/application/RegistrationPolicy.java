package com.longdnh.donot_get_left_behind.identity.application;

import static com.longdnh.donot_get_left_behind.common.Validation.requireNonBlank;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.longdnh.donot_get_left_behind.identity.domain.User;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;

public final class RegistrationPolicy {

    public static final int MIN_PASSWORD_LENGTH = 10;
    public static final int MAX_PASSWORD_LENGTH = 128;

    private final Validator validator;
    private final Set<String> blockedPasswords;

    public RegistrationPolicy(Validator validator, Set<String> blockedPasswords) {
        this.validator = Objects.requireNonNull(validator, "validator is required");
        this.blockedPasswords = Objects.requireNonNull(blockedPasswords, "blockedPasswords is required")
                .stream()
                .map(password -> password.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (this.blockedPasswords.isEmpty()) {
            throw new IllegalArgumentException("The common-password blocklist must not be empty");
        }
    }

    public String canonicalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email is required");
        }
        String canonicalEmail = requireNonBlank(email, "email").strip().toLowerCase(Locale.ROOT);
        if (canonicalEmail.codePointCount(0, canonicalEmail.length()) > User.MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("email must not exceed " + User.MAX_EMAIL_LENGTH + " characters");
        }
        if (!validator.validate(new EmailInput(canonicalEmail)).isEmpty()) {
            throw new IllegalArgumentException("email must be a valid email address");
        }
        return canonicalEmail;
    }

    public void validatePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password is required");
        }
        int length = password.codePointCount(0, password.length());
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("password must contain between "
                    + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters");
        }
        // Case folding is only for the blocklist comparison; the original password is hashed.
        if (blockedPasswords.contains(password.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("password is too common");
        }
    }

    private record EmailInput(@Email String value) {
    }
}
