package com.durgasoft.redis.exception;

public class ProductNotFoundException extends RuntimeException {
    /** Creates the domain exception used when a requested product does not exist. */
    public ProductNotFoundException(Long id) {
        super("Product with id " + id + " was not found");
    }
}
