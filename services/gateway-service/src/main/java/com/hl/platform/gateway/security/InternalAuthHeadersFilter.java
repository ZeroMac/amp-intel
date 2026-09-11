package com.hl.platform.gateway.security;

import java.util.ArrayList;
import java.util.Locale;
import com.hl.platform.base.security.AuthHeaders;
import com.hl.platform.base.security.InternalAuthSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class InternalAuthHeadersFilter implements GlobalFilter, Ordered {
    private final InternalAuthSigner signer;

    public InternalAuthHeadersFilter(@Value("${security.internal.secret}") String secret) {
        this.signer = new InternalAuthSigner(secret);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken token && token.isAuthenticated())
                .cast(JwtAuthenticationToken.class)
                .map(token -> exchange.mutate().request(exchange.getRequest().mutate().headers(headers -> {
                    clearInternalHeaders(headers);
                    var jwt = token.getToken();
                    String userId = jwt.getSubject();
                    String sid = jwt.getClaimAsString("sid");
                    String version = Long.toString(((Number) jwt.getClaim("ver")).longValue());
                    String timestamp = signer.timestamp();
                    headers.set(AuthHeaders.USER_ID, userId);
                    headers.set(AuthHeaders.SID, sid);
                    headers.set(AuthHeaders.TOKEN_VERSION, version);
                    headers.set(AuthHeaders.TIMESTAMP, timestamp);
                    headers.set(AuthHeaders.SIGNATURE, signer.sign(userId, sid, version, timestamp));
                }).build()).build())
                .defaultIfEmpty(exchange.mutate().request(exchange.getRequest().mutate()
                        .headers(InternalAuthHeadersFilter::clearInternalHeaders).build()).build())
                .flatMap(chain::filter);
    }

    private static void clearInternalHeaders(org.springframework.http.HttpHeaders headers) {
        new ArrayList<>(headers.headerNames()).stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith("x-auth-"))
                .forEach(headers::remove);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
