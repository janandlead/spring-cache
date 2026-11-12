package com.durgasoft.redis.service;

import com.durgasoft.redis.dto.ProductRequest;
import com.durgasoft.redis.dto.ProductResponse;
import com.durgasoft.redis.entity.Product;
import com.durgasoft.redis.exception.ProductNotFoundException;
import com.durgasoft.redis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
	private final ProductRepository productRepository;

	/**
	 * Saves a product and invalidates the cached product list because its contents
	 * changed.
	 */
	@Transactional
	@CacheEvict(value = "productList", allEntries = true)
	public ProductResponse create(ProductRequest request) {
		log.info("DB CALL: save new product (INSERT may execute at transaction flush)");
		Product saved = productRepository.save(new Product(request.productName(), request.price()));
		return toResponse(saved);
	}

	/**
	 * Loads one product from PostgreSQL on a cache miss and stores the DTO under
	 * products::{id}. Cache hits are returned by the Spring proxy before this
	 * method executes.
	 */
	/**
	 * Loads and caches the complete product list under productList::all until it
	 * expires or is evicted.
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = "products", key = "#id")
	public ProductResponse getById(Long id) {
		log.info("CACHE MISS | DB CALL: findById({})", id);
		return productRepository.findById(id).map(this::toResponse).orElseThrow(() -> new ProductNotFoundException(id));
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "productList", key = "'all'")
	public List<ProductResponse> getAll() {
		log.info("CACHE MISS | DB CALL: findAll()");
		return productRepository.findAll().stream().map(this::toResponse).toList();
	}

	/**
	 * Updates PostgreSQL, refreshes the individual cache entry, and invalidates the
	 * cached list.
	 */
	@Transactional
	@Caching(put = @CachePut(value = "products", key = "#id"), evict = @CacheEvict(value = "productList", allEntries = true))
	public ProductResponse update(Long id, ProductRequest request) {
		log.info("DB CALL: findById({}) for update", id);
		Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
		product.setProductName(request.productName());
		product.setPrice(request.price());
		log.info("DB CALL: save product {} (UPDATE may execute at transaction flush)", id);
		return toResponse(productRepository.save(product));
	}

	/**
	 * Deletes a product and removes both related cache entries; throws 404 when the
	 * product is absent.
	 */
	@Transactional
	@Caching(evict = { @CacheEvict(value = "products", key = "#id"),
			@CacheEvict(value = "productList", allEntries = true) })
	public void delete(Long id) {
		log.info("DB CALL: existsById({}) before delete", id);
		if (!productRepository.existsById(id))
			throw new ProductNotFoundException(id);
		log.info("DB CALL: deleteById({}) (DELETE may execute at transaction flush)", id);
		productRepository.deleteById(id);
	}

	/**
	 * Converts the persistence entity into the DTO exposed by the REST API and
	 * stored in Redis.
	 */
	private ProductResponse toResponse(Product product) {
		return new ProductResponse(product.getId(), product.getProductName(), product.getPrice());
	}
}
