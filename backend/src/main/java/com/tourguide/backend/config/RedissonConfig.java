package com.tourguide.backend.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Builds a {@link RedissonClient} from the standard {@code spring.data.redis.*} properties.
 *
 * <p>Configured manually (rather than via redisson-spring-boot-starter) to avoid coupling the
 * Redisson Spring Data integration version to the Spring Data Redis version. Redisson powers the
 * distributed lock and delayed queue; Lettuce/RedisTemplate handles caching and plain ops.
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties props) {
        Config config = new Config();
        var server = config.useSingleServer()
                .setAddress("redis://" + props.getHost() + ":" + props.getPort());
        if (StringUtils.hasText(props.getPassword())) {
            server.setPassword(props.getPassword());
        }
        if (props.getTimeout() != null) {
            server.setTimeout((int) props.getTimeout().toMillis());
        }
        return Redisson.create(config);
    }
}
