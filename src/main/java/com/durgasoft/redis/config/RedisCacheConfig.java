package com.durgasoft.redis.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisCacheConfig {
    /**
     * Creates the Redis cache manager, JSON value serializer, and cache-specific TTLs.
     * The individual-product cache lasts ten minutes; the product-list cache lasts two minutes.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON is easier to inspect in redis-cli than Java's binary JDK serialization.
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(10));
        Map<String, RedisCacheConfiguration> caches = Map.of(
                "products", defaults.entryTtl(Duration.ofMinutes(10)),
                "productList", defaults.entryTtl(Duration.ofMinutes(2)));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults).withInitialCacheConfigurations(caches).build();
    }
}
