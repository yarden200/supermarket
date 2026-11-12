package com.yarden.orderservice.event;

import java.math.BigDecimal;

public record OrderEventItem(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal priceAtPurchase
) {}