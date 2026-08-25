package com.hl.platform.gateway;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = {
        "security.jwt.secret=" + GatewaySecurityTest.JWT_SECRET,
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false"
})
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    static final String JWT_SECRET = "gateway-test-secret-must-be-at-least-32-bytes";
    private static final String WRONG_SECRET = "another-test-secret-that-is-at-least-32-bytes";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void publicPathAllowsAnonymousRequests() {
        webTestClient.get()
                .uri("/auth/test")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void protectedPathWithoutTokenReturnsUnauthorizedJson() {
        assertUnauthorized(webTestClient.get()
                .uri("/api/system/test")
                .exchange());
    }

    @Test
    void invalidJwtReturnsUnauthorizedJson() {
        assertUnauthorized(webTestClient.get()
                .uri("/api/system/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .exchange());
    }

    @Test
    void jwtSignedWithAnotherSecretReturnsUnauthorizedJson() throws Exception {
        String token = createToken(WRONG_SECRET, Instant.now().plusSeconds(300));

        assertUnauthorized(webTestClient.get()
                .uri("/api/system/test")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange());
    }

    @Test
    void expiredJwtReturnsUnauthorizedJson() throws Exception {
        String token = createToken(JWT_SECRET, Instant.now().minusSeconds(60));

        assertUnauthorized(webTestClient.get()
                .uri("/api/system/test")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange());
    }

    @Test
    void validJwtPassesSecurityAndReachesGatewayRoute() throws Exception {
        String token = createToken(JWT_SECRET, Instant.now().plusSeconds(300));

        webTestClient.get()
                .uri("/api/system/test")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    private static void assertUnauthorized(WebTestClient.ResponseSpec response) {
        response.expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Unauthorized");
    }

    private static String createToken(String secret, Instant expiresAt) throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(5);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("12345")
                .claim("sid", "test-session-id")
                .claim("ver", 1)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
