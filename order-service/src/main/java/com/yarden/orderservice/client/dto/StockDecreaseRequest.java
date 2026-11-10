package com.yarden.orderservice.client.dto;

import java.util.List;

public record StockDecreaseRequest(
        List<StockItemRequest> items
) {
}
