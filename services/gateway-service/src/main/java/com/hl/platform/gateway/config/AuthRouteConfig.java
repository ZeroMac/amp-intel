package com.hl.platform.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AuthRouteConfig {
    @Bean
    RouteLocator logoutRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("system-auth-logout", route -> route.order(-100)
                        .path("/api/auth/logout").and().method("POST")
                        .uri("lb://system-service"))
                .build();
    }
}
