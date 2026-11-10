package com.yarden.orderservice.mapper;

import com.yarden.orderservice.dto.OrderItemResponse;
import com.yarden.orderservice.dto.OrderResponse;
import com.yarden.orderservice.model.Order;
import com.yarden.orderservice.model.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return toResponse(order, Map.of());
    }

    public OrderResponse toResponse(Order order, Map<Long, Integer> remainingStock) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> toItemResponse(item, remainingStock.get(item.getProductId())))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item, Integer remainingStock) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPriceAtPurchase(),
                item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())),
                remainingStock
        );
    }
}