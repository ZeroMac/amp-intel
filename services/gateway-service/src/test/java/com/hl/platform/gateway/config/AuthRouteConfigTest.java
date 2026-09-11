package com.hl.platform.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.predicate.MethodRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;

class AuthRouteConfigTest {
    @Test
    void logoutIsForwardedToSystemWithoutPathRewriting() {
        try (var context = new GenericApplicationContext()) {
            context.registerBean(org.springframework.boot.webflux.autoconfigure.WebFluxProperties.class);
            context.registerBean(PathRoutePredicateFactory.class);
            context.registerBean(MethodRoutePredicateFactory.class);
            context.refresh();
            var routes = new AuthRouteConfig().logoutRouteLocator(new RouteLocatorBuilder(context));
            StepVerifier.create(routes.getRoutes()).assertNext(route -> {
                assertThat(route.getUri().toString()).isEqualTo("lb://system-service");
                assertThat(route.getFilters()).isEmpty();
                StepVerifier.create(route.getPredicate().apply(MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/auth/logout")))).expectNext(true).verifyComplete();
                StepVerifier.create(route.getPredicate().apply(MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/auth/login")))).expectNext(false).verifyComplete();
            }).verifyComplete();
        }
    }
}
