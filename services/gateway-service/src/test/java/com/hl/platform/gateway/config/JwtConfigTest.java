package com.hl.platform.gateway.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> jwtConfig.jwtDecoder(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("security.jwt.secret must be configured via JWT_SECRET");
    }

    @Test
    void rejectsSecretShorterThan256Bits() {
        assertThatThrownBy(() -> jwtConfig.jwtDecoder("short-secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("security.jwt.secret must contain at least 32 bytes for HS256");
    }
}
