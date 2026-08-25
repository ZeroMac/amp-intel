package com.hl.platform.gateway.config;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import com.hl.platform.gateway.security.AuthSessionValidator;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;

import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    private static final byte[] UNAUTHORIZED_BODY =
            "{\"code\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FORBIDDEN_BODY =
            "{\"code\":403,\"message\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SERVICE_UNAVAILABLE_BODY =
            "{\"code\":503,\"message\":\"Service Unavailable\"}".getBytes(StandardCharsets.UTF_8);

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveAuthenticationManager authenticationManager,
            ServerAuthenticationEntryPoint authenticationEntryPoint,
            ServerAccessDeniedHandler accessDeniedHandler) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/login", "/auth/refresh", "/actuator/health").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(exchange -> Mono.just(authenticationManager))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .authenticationFailureHandler(authenticationFailureHandler(authenticationEntryPoint))
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

    @Bean
    ReactiveAuthenticationManager authenticationManager(
            ReactiveJwtDecoder jwtDecoder,
            AuthSessionValidator authSessionValidator) {
        JwtReactiveAuthenticationManager jwtAuthenticationManager =
                new JwtReactiveAuthenticationManager(jwtDecoder);
        return authentication -> jwtAuthenticationManager.authenticate(authentication)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authenticated -> authSessionValidator.validate(authenticated.getToken())
                        .thenReturn(authenticated));
    }

    @Bean
    ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, exception) -> exception instanceof AuthenticationServiceException
                ? writeJson(exchange.getResponse(), HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE_BODY)
                : writeJson(exchange.getResponse(), HttpStatus.UNAUTHORIZED, UNAUTHORIZED_BODY);
    }

    private ServerAuthenticationFailureHandler authenticationFailureHandler(
            ServerAuthenticationEntryPoint authenticationEntryPoint) {
        return (webFilterExchange, exception) ->
                authenticationEntryPoint.commence(webFilterExchange.getExchange(), exception);
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
