package com.durgasoft.redis;

import com.durgasoft.redis.config.RedisCacheConfig;
import com.durgasoft.redis.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheConfigTest {
    @Test
    void configuredSerializersRoundTripProductsAndStreamLists() {
        // Configuration and serialization do not require a running Redis server.
        RedisCacheManager manager = (RedisCacheManager) new RedisCacheConfig()
                .cacheManager(new LettuceConnectionFactory());
        manager.afterPropertiesSet();
        ProductResponse product = new ProductResponse(1L, "Keyboard", 50.0);
        var products = manager.getCacheConfigurations().get("products");
        var items = products.getValueSerializationPair();
        assertThat(items.read(items.write(product))).isEqualTo(product);

        var lists = manager.getCacheConfigurations().get("productList").getValueSerializationPair();
        List<ProductResponse> value = List.of(product).stream().toList();
        assertThat(lists.read(lists.write(value))).isEqualTo(value);
        assertThat(lists.read(lists.write(List.of()))).isEqualTo(List.of());
        assertThat(products.getKeyPrefixFor("products")).isEqualTo("spring-cache:v2:products::");
    }
}
