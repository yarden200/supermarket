package com.yarden.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "CustomerId is required")
        Long customerId,

        @NotEmpty(message = "Items must not be empty")
        List<@Valid OrderItemRequest> items
) {}
