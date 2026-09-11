package com.hl.platform.gateway.security;

import com.hl.platform.base.security.AuthHeaders;
import com.hl.platform.base.security.InternalAuthSigner;
import com.hl.platform.gateway.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GatewayAuthenticationChainTest {
    private static final String SECRET = "test-only-internal-secret-32-bytes-long";
    private AnnotationConfigApplicationContext context;
    private AuthSessionService sessions;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        sessions = context.getBean(AuthSessionService.class);
        var jwt = Jwt.withTokenValue("valid").header("alg", "HS256")
                .subject("100").claim("sid", "session-a").claim("ver", 2).build();
        when(context.getBean(ReactiveJwtDecoder.class).decode("valid")).thenReturn(Mono.just(jwt));
        var forwarding = new InternalAuthHeadersFilter(SECRET);
        client = WebTestClient.bindToWebHandler(exchange -> forwarding.filter(exchange, downstream -> {
            var headers = downstream.getRequest().getHeaders();
            assertThat(new InternalAuthSigner(SECRET).verify(headers.getFirst(AuthHeaders.USER_ID),
                    headers.getFirst(AuthHeaders.SID), headers.getFirst(AuthHeaders.TOKEN_VERSION),
                    headers.getFirst(AuthHeaders.TIMESTAMP), headers.getFirst(AuthHeaders.SIGNATURE))).isTrue();
            downstream.getResponse().getHeaders().set("Test-User", headers.getFirst(AuthHeaders.USER_ID));
            return downstream.getResponse().setComplete();
        })).webFilter(new WebFilterChainProxy(context.getBean(SecurityWebFilterChain.class))).build();
    }

    @AfterEach
    void close() { context.close(); }

    @Test
    void jwtAndActiveSessionProduceSignedIdentity() {
        when(sessions.get("100", "session-a"))
                .thenReturn(Mono.just(new AuthSession("100", "test", 2, "ACTIVE")));
        client.get().uri("/sys/functions/0/children").headers(headers -> {
            headers.setBearerAuth("valid");
            headers.set(AuthHeaders.USER_ID, "victim");
        }).exchange().expectStatus().isOk().expectHeader().valueEquals("Test-User", "100");
    }

    @Test
    void forgedHeadersWithoutBearerCannotPassGateway() {
        client.get().uri("/sys/functions/0/children").header(AuthHeaders.USER_ID, "100")
                .header(AuthHeaders.SID, "session-a").header(AuthHeaders.TOKEN_VERSION, "2")
                .exchange().expectStatus().isUnauthorized();
        verifyNoInteractions(sessions);
    }

    @Test
    void missingSessionOrChangedVersionCannotReachDownstream() {
        when(sessions.get("100", "session-a")).thenReturn(Mono.empty());
        request().expectStatus().isUnauthorized();
        when(sessions.get("100", "session-a"))
                .thenReturn(Mono.just(new AuthSession("100", "test", 3, "ACTIVE")));
        request().expectStatus().isUnauthorized();
    }

    @Test
    void sessionStoreFailureCannotReachDownstream() {
        when(sessions.get("100", "session-a")).thenReturn(Mono.error(new IllegalStateException("offline")));
        request().expectStatus().isEqualTo(503);
    }

    private WebTestClient.ResponseSpec request() {
        return client.get().uri("/sys/functions/0/children")
                .headers(headers -> headers.setBearerAuth("valid")).exchange();
    }

    @Configuration
    @EnableWebFluxSecurity
    @Import({SecurityConfig.class, AuthSessionValidator.class})
    static class TestConfig {
        @Bean ReactiveJwtDecoder decoder() { return mock(ReactiveJwtDecoder.class); }
        @Bean AuthSessionService sessions() { return mock(AuthSessionService.class); }
    }
}
