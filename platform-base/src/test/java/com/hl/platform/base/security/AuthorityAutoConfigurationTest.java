package com.hl.platform.base.security;

import com.hl.platform.base.security.AuthorityCacheReader;
import com.hl.platform.base.security.PlatformSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthorityAutoConfigurationTest {
    @Test
    void readerIsCreatedAfterRedisAutoConfigurationWithoutConnectingToRedis() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformSecurityAutoConfiguration.class,
                        DataRedisAutoConfiguration.class, JacksonAutoConfiguration.class))
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .run(context -> assertThat(context).hasSingleBean(AuthorityCacheReader.class));
    }
}
