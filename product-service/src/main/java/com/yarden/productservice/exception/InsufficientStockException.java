package com.yarden.productservice.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final List<StockShortage> shortages;

    public InsufficientStockException(List<StockShortage> shortages) {
        super("Insufficient stock for some products");
        this.shortages = shortages;
    }

    public record StockShortage(Long productId, int requested, int available) {}
}
