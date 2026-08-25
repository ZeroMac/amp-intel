package com.hl.platform.gateway.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class JwtConfig {

    private static final int MINIMUM_SECRET_LENGTH_BYTES = 32;

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        if (secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be configured via JWT_SECRET");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException("security.jwt.secret must contain at least 32 bytes for HS256");
        }

        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }
}
