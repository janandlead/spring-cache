package com.durgasoft.redis;

import com.durgasoft.redis.dto.ProductRequest;
import com.durgasoft.redis.dto.ProductResponse;
import com.durgasoft.redis.entity.Product;
import com.durgasoft.redis.repository.ProductRepository;
import com.durgasoft.redis.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = ProductServiceTest.Config.class)
class ProductServiceTest {
    @TestConfiguration
    @EnableCaching
    static class Config {
        /** Creates the service under test inside a Spring context so cache proxies are active. */
        @Bean ProductService productService(ProductRepository repository) { return new ProductService(repository); }
        /** Uses an in-memory cache for fast tests while preserving Spring Cache interception. */
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager("products", "productList"); }
    }

    @Autowired ProductService service;
    @Autowired CacheManager cacheManager;
    @MockBean ProductRepository repository;

    /** Clears shared test caches so each test starts with a cache miss. */
    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    /** Verifies that two reads of the same key call the repository only once. */
    @Test
    void cacheHitCallsRepositoryOnlyOnce() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Product(1L, "Keyboard", 50.0)));
        ProductResponse first = service.getById(1L);
        ProductResponse second = service.getById(1L);
        assertThat(first).isEqualTo(second);
        verify(repository, times(1)).findById(1L);
    }

    /** Verifies that an update refreshes the item cache and evicts the list cache. */
    @Test
    void updateRefreshesIndividualCacheAndEvictsList() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Product(1L, "Old", 10.0)));
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.getById(1L);
        ProductResponse updated = service.update(1L, new ProductRequest("New", 20.0));
        assertThat(updated.productName()).isEqualTo("New");
        assertThat(cacheManager.getCache("products").get(1L).get()).isEqualTo(updated);
        verify(repository).save(any(Product.class));
    }

    /** Verifies that deleting a product removes its individual cache entry. */
    @Test
    void deleteEvictsIndividualCache() {
        when(repository.existsById(1L)).thenReturn(true);
        cacheManager.getCache("products").put(1L, new ProductResponse(1L, "Keyboard", 50.0));
        service.delete(1L);
        assertThat(cacheManager.getCache("products").get(1L)).isNull();
        verify(repository).deleteById(1L);
    }
}
