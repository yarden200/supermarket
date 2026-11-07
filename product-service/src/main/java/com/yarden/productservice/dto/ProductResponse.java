package com.yarden.productservice.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        CategoryResponse category,
        String imageUrl
) {
}
