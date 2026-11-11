package com.yarden.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Items must not be empty")
        List<@Valid OrderItemRequest> items
) {}
