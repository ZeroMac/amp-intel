package com.hl.platform.gateway.security;

import java.util.concurrent.atomic.AtomicReference;
import com.hl.platform.base.security.AuthHeaders;
import com.hl.platform.base.security.InternalAuthSigner;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;

class InternalAuthHeadersFilterTest {
    private static final String SECRET = "test-only-internal-secret-32-bytes-long";
    private final InternalAuthHeadersFilter filter = new InternalAuthHeadersFilter(SECRET);

    @Test
    void onlyValidatedJwtIdentityIsForwardedAndClientHeadersAreRemoved() {
        var jwt = Jwt.withTokenValue("validated-token").header("alg", "HS256")
                .subject("100").claim("sid", "session-a").claim("ver", 2).build();
        var exchange = clientExchange().mutate()
                .principal(Mono.just(new JwtAuthenticationToken(jwt, java.util.List.of()))).build();
        var forwarded = new AtomicReference<ServerWebExchange>();
        StepVerifier.create(filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        })).verifyComplete();
        var headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.get(AuthHeaders.USER_ID)).containsExactly("100");
        assertThat(headers.getFirst(AuthHeaders.SID)).isEqualTo("session-a");
        assertThat(headers.getFirst(AuthHeaders.TOKEN_VERSION)).isEqualTo("2");
        assertThat(headers.containsHeader("X-Auth-Authorities")).isFalse();
        assertThat(new InternalAuthSigner(SECRET).verify("100", "session-a", "2",
                headers.getFirst(AuthHeaders.TIMESTAMP), headers.getFirst(AuthHeaders.SIGNATURE))).isTrue();
    }

    @Test
    void anonymousRoutesNeverForwardClientInternalHeaders() {
        StepVerifier.create(filter.filter(clientExchange(), next -> {
            assertThat(next.getRequest().getHeaders().headerNames())
                    .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith("x-auth-"));
            return Mono.empty();
        })).verifyComplete();
    }

    private ServerWebExchange clientExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/sys/functions/0/children")
                .header("x-auth-user-id", "attacker", "victim")
                .header(AuthHeaders.SID, "forged").header(AuthHeaders.TOKEN_VERSION, "99")
                .header(AuthHeaders.SIGNATURE, "forged").header(AuthHeaders.TIMESTAMP, "1")
                .header("X-Auth-Authorities", "sys:func:read"));
    }
}
