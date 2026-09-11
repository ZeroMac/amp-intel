package com.hl.platform.base.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.servlet.Servlet;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(
        afterName = {"org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
                "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration"},
        beforeName = "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
public class PlatformSecurityAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({StringRedisTemplate.class, ObjectMapper.class})
    static class AuthorityCacheConfiguration {
        @Bean
        @ConditionalOnBean({StringRedisTemplate.class, ObjectMapper.class})
        @ConditionalOnMissingBean(AuthorityCacheReader.class)
        AuthorityCacheReader authorityCacheReader(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
            return new RedisAuthorityCacheReader(redisTemplate, objectMapper);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass({Servlet.class, HttpSecurity.class, SecurityFilterChain.class})
    @EnableWebSecurity
    @EnableMethodSecurity
    static class ServletSecurityConfiguration {
        @Bean
        @ConditionalOnMissingBean(InternalAuthSigner.class)
        InternalAuthSigner internalAuthSigner(@Value("${security.internal.secret}") String secret) {
            return new InternalAuthSigner(secret);
        }

        @Bean
        @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
        AuthenticationEntryPoint authenticationEntryPoint() {
            return (request, response, exception) ->
                    InternalAuthenticationFilter.writeError(response, 401, "Unauthorized");
        }

        @Bean
        @ConditionalOnMissingBean(AccessDeniedHandler.class)
        AccessDeniedHandler accessDeniedHandler() {
            return (request, response, exception) ->
                    InternalAuthenticationFilter.writeError(response, 403, "Forbidden");
        }

        @Bean
        @ConditionalOnMissingBean(InternalAuthenticationFilter.class)
        InternalAuthenticationFilter internalAuthenticationFilter(AuthorityCacheReader reader,
                InternalAuthSigner signer, AuthenticationEntryPoint entryPoint) {
            return new InternalAuthenticationFilter(reader, signer, entryPoint);
        }

        // A Filter bean must run only inside Spring Security, never as a second servlet filter.
        @Bean
        @ConditionalOnMissingBean(name = "internalAuthenticationFilterRegistration")
        FilterRegistrationBean<InternalAuthenticationFilter> internalAuthenticationFilterRegistration(
                InternalAuthenticationFilter filter) {
            var registration = new FilterRegistrationBean<>(filter);
            registration.setEnabled(false);
            return registration;
        }

        @Bean
        @ConditionalOnMissingBean(SecurityFilterChain.class)
        SecurityFilterChain securityFilterChain(HttpSecurity http, InternalAuthenticationFilter filter,
                AuthenticationEntryPoint entryPoint, AccessDeniedHandler deniedHandler) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requestCache(cache -> cache.disable())
                    .addFilterBefore(filter, AnonymousAuthenticationFilter.class)
                    .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint)
                            .accessDeniedHandler(deniedHandler))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }
}
