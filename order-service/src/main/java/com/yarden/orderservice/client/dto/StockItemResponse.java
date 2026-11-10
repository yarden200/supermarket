package com.yarden.orderservice.client.dto;

import java.math.BigDecimal;

public record StockItemResponse(Long productId, String name, BigDecimal price, Integer remainingStock) {}
