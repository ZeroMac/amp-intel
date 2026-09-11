package com.hl.platform.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hl.platform.base.security.AuthHeaders;
import com.hl.platform.base.security.AuthorityCacheReader;
import com.hl.platform.base.security.InternalAuthSigner;
import com.hl.platform.base.security.PlatformSecurityAutoConfiguration;
import com.hl.platform.system.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(LogoutTest.TestConfig.class)
@TestPropertySource(properties = "security.internal.secret=" + LogoutTest.SECRET)
class LogoutTest {
    static final String SECRET = "test-only-internal-secret-32-bytes-long";
    @Autowired WebApplicationContext context;
    @Autowired StringRedisTemplate redis;
    private MockMvc mvc;
    private Map<String, String> stored;

    @BeforeEach
    void setUp() {
        reset(redis);
        stored = new HashMap<>(Map.of("auth:100:session:session-a", "current",
                "auth:100:session:session-b", "other session", "auth:200:session:session-a", "other user",
                "auth:100:authority", "[]", "auth:100:function", "[]"));
        when(redis.delete(anyString())).thenAnswer(invocation -> stored.remove(invocation.getArgument(0)) != null);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void deletesOnlyAuthenticatedSessionAndIgnoresClientSuppliedIdentity() throws Exception {
        mvc.perform(signed().param("userId", "200").param("sid", "session-b"))
                .andExpect(status().isNoContent());
        assertThat(stored).containsExactlyInAnyOrderEntriesOf(Map.of(
                "auth:100:session:session-b", "other session", "auth:200:session:session-a", "other user",
                "auth:100:authority", "[]", "auth:100:function", "[]"));
        verify(redis).delete("auth:100:session:session-a");
        verifyNoMoreInteractions(redis);
    }

    @Test
    void repeatedLogoutAndMissingSessionSucceed() throws Exception {
        mvc.perform(signed()).andExpect(status().isNoContent());
        mvc.perform(signed()).andExpect(status().isNoContent());
        verify(redis, times(2)).delete("auth:100:session:session-a");
        verifyNoMoreInteractions(redis);
    }

    @Test
    void unsignedRequestsCannotDeleteSessions() throws Exception {
        mvc.perform(post("/api/auth/logout").header(AuthHeaders.USER_ID, "100")
                .header(AuthHeaders.SID, "session-a").header(AuthHeaders.TOKEN_VERSION, "2"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(redis);
    }

    @Test
    void redisFailureDoesNotReportSuccessfulLogout() throws Exception {
        doThrow(new RedisConnectionFailureException("offline"))
                .when(redis).delete("auth:100:session:session-a");
        mvc.perform(signed()).andExpect(status().isServiceUnavailable());
        assertThat(stored).containsKey("auth:100:session:session-a");
    }

    private MockHttpServletRequestBuilder signed() {
        var signer = new InternalAuthSigner(SECRET);
        String timestamp = signer.timestamp();
        return post("/api/auth/logout").header(AuthHeaders.USER_ID, "100")
                .header(AuthHeaders.SID, "session-a").header(AuthHeaders.TOKEN_VERSION, "2")
                .header(AuthHeaders.TIMESTAMP, timestamp)
                .header(AuthHeaders.SIGNATURE, signer.sign("100", "session-a", "2", timestamp));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({AuthController.class, AuthServiceImpl.class})
    @ImportAutoConfiguration(PlatformSecurityAutoConfiguration.class)
    static class TestConfig {
        @Bean StringRedisTemplate redis() { return mock(StringRedisTemplate.class); }
        @Bean AuthorityCacheReader authorities() { return userId -> List.of(); }
    }
}
