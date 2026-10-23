package com.longdnh.donot_get_left_behind.identity.infrastructure.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.longdnh.donot_get_left_behind.identity.application.RegistrationPolicy;

import jakarta.validation.Validator;

@Configuration(proxyBeanMethods = false)
public class IdentityConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Salt/hash sizes are bytes; memory is KiB: 19 MiB, two iterations, parallelism one.
        PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2));
    }

    @Bean
    public RegistrationPolicy registrationPolicy(Validator validator) throws IOException {
        ClassPathResource resource = new ClassPathResource("security/common-passwords.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            Set<String> blockedPasswords = reader.lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .collect(Collectors.toUnmodifiableSet());
            return new RegistrationPolicy(validator, blockedPasswords);
        }
    }
}
