package com.hl.platform.gateway;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import com.hl.platform.gateway.cache.AuthCacheSerializationException;
import com.hl.platform.gateway.security.AuthSession;
import com.hl.platform.gateway.security.AuthSessionService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private static final String USER_ID = "100";
    private static final String SID = "session-a";
    private static final AuthSession ACTIVE_SESSION = new AuthSession(USER_ID, "admin", 1, "ACTIVE");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuthSessionService authSessionService;

    @Test
    void publicLoginPathAllowsAnonymousRequests() {
        webTestClient.get().uri("/auth/login").exchange().expectStatus().isNotFound();
    }

    @Test
    void logoutRequiresAuthentication() {
        assertUnauthorized(webTestClient.post().uri("/auth/logout").exchange());
    }

    @Test
    void protectedPathWithoutTokenReturnsUnauthorizedJson() {
        assertUnauthorized(webTestClient.get().uri("/api/system/test").exchange());
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
        String token = createToken(WRONG_SECRET, USER_ID, SID, 1, Instant.now().plusSeconds(300));
        assertUnauthorized(authenticatedGet(token));
    }

    @Test
    void expiredJwtReturnsUnauthorizedJson() throws Exception {
        String token = createToken(JWT_SECRET, USER_ID, SID, 1, Instant.now().minusSeconds(60));
        assertUnauthorized(authenticatedGet(token));
    }

    @Test
    void activeSessionPassesSecurityAndReachesGatewayRoute() throws Exception {
        when(authSessionService.get(USER_ID, SID)).thenReturn(Mono.just(ACTIVE_SESSION));

        authenticatedGet(validToken()).expectStatus().isEqualTo(503);
        verify(authSessionService).get(USER_ID, SID);
    }

    @Test
    void missingSessionReturnsUnauthorized() throws Exception {
        when(authSessionService.get(USER_ID, SID)).thenReturn(Mono.empty());
        assertUnauthorized(authenticatedGet(validToken()));
    }

    @Test
    void sessionWithDifferentUserIdReturnsUnauthorized() throws Exception {
        when(authSessionService.get(USER_ID, SID))
                .thenReturn(Mono.just(new AuthSession("200", "admin", 1, "ACTIVE")));
        assertUnauthorized(authenticatedGet(validToken()));
    }

    @Test
    void sessionWithDifferentTokenVersionReturnsUnauthorized() throws Exception {
        when(authSessionService.get(USER_ID, SID))
                .thenReturn(Mono.just(new AuthSession(USER_ID, "admin", 2, "ACTIVE")));
        assertUnauthorized(authenticatedGet(validToken()));
    }

    @Test
    void nonActiveSessionReturnsUnauthorized() throws Exception {
        when(authSessionService.get(USER_ID, SID))
                .thenReturn(Mono.just(new AuthSession(USER_ID, "admin", 1, "DISABLED")));
        assertUnauthorized(authenticatedGet(validToken()));
    }

    @Test
    void damagedSessionJsonReturnsUnauthorized() throws Exception {
        when(authSessionService.get(USER_ID, SID)).thenReturn(Mono.error(
                new AuthCacheSerializationException("damaged", new IllegalArgumentException("invalid JSON"))));
        assertUnauthorized(authenticatedGet(validToken()));
    }

    @Test
    void missingSubjectReturnsUnauthorized() throws Exception {
        String token = createToken(JWT_SECRET, null, SID, 1, Instant.now().plusSeconds(300));
        assertUnauthorized(authenticatedGet(token));
    }

    @Test
    void missingSidReturnsUnauthorized() throws Exception {
        String token = createToken(JWT_SECRET, USER_ID, null, 1, Instant.now().plusSeconds(300));
        assertUnauthorized(authenticatedGet(token));
    }

    @Test
    void missingVersionReturnsUnauthorized() throws Exception {
        String token = createToken(JWT_SECRET, USER_ID, SID, null, Instant.now().plusSeconds(300));
        assertUnauthorized(authenticatedGet(token));
    }

    @Test
    void logoutDeletesCurrentSessionAndOldJwtImmediatelyFails() throws Exception {
        when(authSessionService.get(USER_ID, SID))
                .thenReturn(Mono.just(ACTIVE_SESSION), Mono.empty());
        when(authSessionService.invalidate(USER_ID, SID)).thenReturn(Mono.just(true));
        String token = validToken();

        webTestClient.post()
                .uri("/auth/logout")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isNoContent();
        verify(authSessionService).invalidate(USER_ID, SID);
        assertUnauthorized(authenticatedGet(token));
    }

    @Test
    void changedTokenVersionImmediatelyInvalidatesOldJwt() throws Exception {
        when(authSessionService.get(USER_ID, SID)).thenReturn(
                Mono.just(ACTIVE_SESSION),
                Mono.just(new AuthSession(USER_ID, "admin", 2, "ACTIVE")));
        String token = validToken();

        authenticatedGet(token).expectStatus().isEqualTo(503);
        assertUnauthorized(authenticatedGet(token));
    }

    @Test
    void sessionStoreFailureReturnsServiceUnavailableAndNeverRoutes() throws Exception {
        when(authSessionService.get(USER_ID, SID))
                .thenReturn(Mono.error(new RuntimeException("Redis unavailable")));

        authenticatedGet(validToken())
                .expectStatus().isEqualTo(503)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(503)
                .jsonPath("$.message").isEqualTo("Service Unavailable");
    }

    private WebTestClient.ResponseSpec authenticatedGet(String token) {
        return webTestClient.get()
                .uri("/api/system/test")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange();
    }

    private static void assertUnauthorized(WebTestClient.ResponseSpec response) {
        response.expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Unauthorized");
    }

    private static String validToken() throws Exception {
        return createToken(JWT_SECRET, USER_ID, SID, 1, Instant.now().plusSeconds(300));
    }

    private static String createToken(
            String secret,
            String subject,
            String sid,
            Integer version,
            Instant expiresAt) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issueTime(Date.from(Instant.now().minusSeconds(5)))
                .expirationTime(Date.from(expiresAt));
        if (subject != null) {
            claims.subject(subject);
        }
        if (sid != null) {
            claims.claim("sid", sid);
        }
        if (version != null) {
            claims.claim("ver", version);
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
