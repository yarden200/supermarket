package com.yarden.searchservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderEvent(
        OrderEventType type,
        Long orderId,
        Long customerId,
        String status,
        BigDecimal totalPrice,
        List<OrderEventItem> items,
        Instant occurredAt
) {}