package com.yarden.searchservice.event;

import java.math.BigDecimal;

public record OrderEventItem(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal priceAtPurchase) {
}