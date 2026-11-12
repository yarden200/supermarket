package com.yarden.orderservice.event;

import com.yarden.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderEvent(
        OrderEventType type,
        Long orderId,
        Long customerId,
        OrderStatus status,
        BigDecimal totalPrice,
        List<OrderEventItem> items,
        Instant occurredAt
) {}