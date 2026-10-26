package com.longdnh.donot_get_left_behind.identity.infrastructure.configuration;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    @Bean
    public JwtEncoder jwtEncoder(
            @Value("${app.auth.jwt-private-key}") Resource privateKeyResource,
            @Value("${app.auth.jwt-public-key}") Resource publicKeyResource) throws IOException {
        RSAPrivateKey privateKey;
        RSAPublicKey publicKey;

        try (var input = privateKeyResource.getInputStream()) {
            privateKey = Objects.requireNonNull(
                    RsaKeyConverters.pkcs8().convert(input),
                    "RSA private key is required");
        }

        try (var input = publicKeyResource.getInputStream()) {
            publicKey = Objects.requireNonNull(
                    RsaKeyConverters.x509().convert(input),
                    "RSA public key is required");
        }

        if (!privateKey.getModulus().equals(publicKey.getModulus())) {
            throw new IllegalStateException(
                    "JWT public and private keys do not match");
        }

        return NimbusJwtEncoder.withKeyPair(publicKey, privateKey)
                .build();
    }
}
