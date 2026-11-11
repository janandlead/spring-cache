package com.durgasoft.redis.service;

import com.durgasoft.redis.dto.ProductRequest;
import com.durgasoft.redis.dto.ProductResponse;
import com.durgasoft.redis.entity.Product;
import com.durgasoft.redis.exception.ProductNotFoundException;
import com.durgasoft.redis.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    /** Injects the repository used for PostgreSQL persistence. */
    public ProductService(ProductRepository productRepository) { this.productRepository = productRepository; }

    /** Saves a product and invalidates the cached product list because its contents changed. */
    @Transactional
    @CacheEvict(value = "productList", allEntries = true)
    public ProductResponse create(ProductRequest request) {
        Product saved = productRepository.save(new Product(request.productName(), request.price()));
        return toResponse(saved);
    }

    /**
     * Loads one product from PostgreSQL on a cache miss and stores the DTO under products::{id}.
     * Cache hits are returned by the Spring proxy before this method executes.
     */
    /** Loads and caches the complete product list under productList::all until it expires or is evicted. */
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getById(Long id) {
        log.info("CACHE MISS: querying PostgreSQL for product {}", id);
        return productRepository.findById(id).map(this::toResponse).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productList", key = "'all'")
    public List<ProductResponse> getAll() {
        log.info("CACHE MISS: querying PostgreSQL for all products");
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Updates PostgreSQL, refreshes the individual cache entry, and invalidates the cached list. */
    @Transactional
    @Caching(
            put = @CachePut(value = "products", key = "#id"),
            evict = @CacheEvict(value = "productList", allEntries = true))
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setProductName(request.productName());
        product.setPrice(request.price());
        return toResponse(productRepository.save(product));
    }

    /** Deletes a product and removes both related cache entries; throws 404 when the product is absent. */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productList", allEntries = true)
    })
    public void delete(Long id) {
        if (!productRepository.existsById(id)) throw new ProductNotFoundException(id);
        productRepository.deleteById(id);
    }

    /** Converts the persistence entity into the DTO exposed by the REST API and stored in Redis. */
    private ProductResponse toResponse(Product product) { return new ProductResponse(product.getId(), product.getProductName(), product.getPrice()); }
}
