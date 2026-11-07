package com.yarden.productservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "sku is required")
        @Size(max = 50, message = "sku must not exceed 50 characters")
        String sku,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        @DecimalMax(value = "100000.00", message = "Price must not exceed 100000")
        @Digits(integer = 6, fraction = 2, message = "Price must have up to 2 decimal places")
        BigDecimal price,

        @NotNull(message = "StockQuantity is required")
        @PositiveOrZero(message = "StockQuantity must be zero or positive")
        Integer stockQuantity,

        @NotNull(message = "CategoryId is required")
        Long categoryId
) {
}
