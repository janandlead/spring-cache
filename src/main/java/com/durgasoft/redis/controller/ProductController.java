package com.durgasoft.redis.controller;

import com.durgasoft.redis.dto.ProductRequest;
import com.durgasoft.redis.dto.ProductResponse;
import com.durgasoft.redis.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
	private final ProductService productService;

	@PostMapping
	/** Creates a product and returns HTTP 201 with a Location header for the new resource. */
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
		ProductResponse response = productService.create(request);
		return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
	}

	@GetMapping("/{id}")
	/** Returns one product; the service cache may satisfy the request without a database query. */
	public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.getById(id));
	}

	@GetMapping
	/** Returns all products, using the product-list cache when available. */
	public ResponseEntity<List<ProductResponse>> getAll() {
		return ResponseEntity.ok(productService.getAll());
	}

	@PutMapping("/{id}")
	/** Updates a product, refreshes its item cache, and invalidates the list cache. */
	public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return ResponseEntity.ok(productService.update(id, request));
	}

	@DeleteMapping("/{id}")
	/** Deletes a product and evicts both its item cache entry and the list cache. */
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		productService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
