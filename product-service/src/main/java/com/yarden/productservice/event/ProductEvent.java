package com.yarden.productservice.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductEvent(
        ProductEventType type,
        Long productId,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Long categoryId,
        String categoryName,
        String imageUrl,
        Instant occurredAt
) {
    public static ProductEvent deleted(Long productId) {
        return new ProductEvent(ProductEventType.DELETED, productId,
                null, null, null, null, null, null, null, null, Instant.now());
    }
}