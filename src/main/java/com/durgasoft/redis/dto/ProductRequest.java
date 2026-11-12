package com.durgasoft.redis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRequest(
        @NotBlank(message = "productName is required") String productName,
        @NotNull(message = "price is required") 
        @PositiveOrZero(message = "price must not be negative") 
        Double price
) { }
