package com.durgasoft.redis.config;

import com.durgasoft.redis.dto.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;
import java.util.List;

@Configuration
public class RedisCacheConfig {
	/**
	 * Creates the Redis cache manager, JSON value serializer, and cache-specific
	 * TTLs. The individual-product cache lasts ten minutes; the product-list cache
	 * lasts two minutes.
	 */
	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		// JSON is easier to inspect in redis-cli than Java's binary JDK serialization.
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
		RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
				.disableCachingNullValues().entryTtl(Duration.ofMinutes(10));
		ObjectMapper mapper = new ObjectMapper();
		Jackson2JsonRedisSerializer<ProductResponse> productSerializer =
				new Jackson2JsonRedisSerializer<>(mapper, ProductResponse.class);
		Jackson2JsonRedisSerializer<List<ProductResponse>> listSerializer =
				new Jackson2JsonRedisSerializer<>(mapper,
						mapper.getTypeFactory().constructCollectionType(List.class, ProductResponse.class));
		// A versioned prefix isolates older entries written with generic type metadata.
		RedisCacheConfiguration typedDefaults = defaults.computePrefixWith(name -> "spring-cache:v2:" + name + "::");
		Map<String, RedisCacheConfiguration> caches = Map.of(
				"products", typedDefaults.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(productSerializer)),
				"productList", typedDefaults.entryTtl(Duration.ofMinutes(2))
						.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(listSerializer)));
		return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults)
				.withInitialCacheConfigurations(caches).build();
	}
}
