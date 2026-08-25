package com.hl.platform.gateway.config;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;

import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    private static final byte[] UNAUTHORIZED_BODY =
            "{\"code\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FORBIDDEN_BODY =
            "{\"code\":403,\"message\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8);

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerAuthenticationEntryPoint authenticationEntryPoint,
            ServerAccessDeniedHandler accessDeniedHandler) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/**", "/actuator/health").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

    @Bean
    ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, exception) -> writeJson(exchange.getResponse(), HttpStatus.UNAUTHORIZED, UNAUTHORIZED_BODY);
    }

    @Bean
    ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, exception) -> writeJson(exchange.getResponse(), HttpStatus.FORBIDDEN, FORBIDDEN_BODY);
    }

    private static Mono<Void> writeJson(
            ServerHttpResponse response,
            HttpStatus status,
            byte[] body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
