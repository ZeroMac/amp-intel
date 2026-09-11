package com.hl.platform.base.security;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformSecurityAutoConfigurationTest {
    private static final String SECRET = "test-only-internal-secret-32-bytes-long";
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformSecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class))
            .withPropertyValues("security.internal.secret=" + SECRET)
            .withBean(AuthorityCacheReader.class, () -> userId -> List.of());

    @Test
    void defaultChainWinsOverBootAndFilterIsNotRegisteredTwice() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(InternalAuthSigner.class)
                    .hasSingleBean(InternalAuthenticationFilter.class).hasSingleBean(SecurityFilterChain.class);
            var filter = context.getBean(InternalAuthenticationFilter.class);
            assertThat(context.getBean(SecurityFilterChain.class).getFilters()).containsOnlyOnce(filter);
            var registration = context.getBean("internalAuthenticationFilterRegistration", FilterRegistrationBean.class);
            assertThat(registration.isEnabled()).isFalse();
            assertThat(registration.getFilter()).isSameAs(filter);
        });
    }

    @Test
    void customSignerFilterHandlersAndChainOverrideDefaults() {
        runner.withUserConfiguration(CustomSecurity.class).run(context -> {
            assertThat(context).hasSingleBean(InternalAuthSigner.class)
                    .hasSingleBean(InternalAuthenticationFilter.class).hasSingleBean(SecurityFilterChain.class)
                    .hasSingleBean(AuthenticationEntryPoint.class).hasSingleBean(AccessDeniedHandler.class);
            assertThat(context.getBean(InternalAuthSigner.class)).isSameAs(context.getBean("customSigner"));
            assertThat(context.getBean(InternalAuthenticationFilter.class)).isSameAs(context.getBean("customFilter"));
            assertThat(context.getBean(SecurityFilterChain.class)).isSameAs(context.getBean("customChain"));
        });
    }

    @Test
    void customUnauthorizedHandlerIsUsedByDefaultFilter() {
        runner.withBean(AuthenticationEntryPoint.class,
                () -> (request, response, exception) -> response.setStatus(418)).run(context -> {
            var response = new MockHttpServletResponse();
            context.getBean(InternalAuthenticationFilter.class).doFilter(new MockHttpServletRequest(), response,
                    (request, nextResponse) -> { throw new AssertionError("Unauthenticated request passed"); });
            assertThat(response.getStatus()).isEqualTo(418);
        });
    }

    @Test
    void servletSecurityDoesNotActivateInReactiveApplication() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformSecurityAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(InternalAuthSigner.class)
                        .doesNotHaveBean(InternalAuthenticationFilter.class).doesNotHaveBean(SecurityFilterChain.class));
    }

    @Test
    void optionalServletSecurityAndRedisClassesAreNotRequiredForBaseConsumers() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("jakarta.servlet", "org.springframework.security",
                        "org.springframework.data.redis", "tools.jackson"))
                .withConfiguration(AutoConfigurations.of(PlatformSecurityAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("internalAuthSigner")
                        .doesNotHaveBean("authorityCacheReader"));
    }

    @Test
    void missingOrWeakSecretFailsClosed() {
        runner.withPropertyValues("security.internal.secret=")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("security.internal.secret=short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSecurity {
        @Bean InternalAuthSigner customSigner() { return new InternalAuthSigner(SECRET); }
        @Bean InternalAuthenticationFilter customFilter() { return mock(InternalAuthenticationFilter.class); }
        @Bean AuthenticationEntryPoint customEntryPoint() {
            return (request, response, exception) -> response.setStatus(401);
        }
        @Bean AccessDeniedHandler customDeniedHandler() {
            return (request, response, exception) -> response.setStatus(403);
        }
        @Bean SecurityFilterChain customChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
    }
}
