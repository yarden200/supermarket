package com.yarden.productservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StockDecreaseRequest(
        @NotEmpty(message = "Items must not be empty")
        List<@Valid StockItemRequest> items
) {
}
