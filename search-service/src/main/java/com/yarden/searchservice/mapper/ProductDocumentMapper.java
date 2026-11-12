package com.yarden.searchservice.mapper;

import com.yarden.searchservice.document.ProductDocument;
import com.yarden.searchservice.event.ProductEvent;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

    public ProductDocument toDocument(ProductEvent event, Long soldCount) {
        return ProductDocument.builder()
                .id(String.valueOf(event.productId()))
                .sku(event.sku())
                .name(event.name())
                .description(event.description())
                .price(event.price() != null ? event.price().doubleValue() : null)
                .stockQuantity(event.stockQuantity())
                .categoryId(event.categoryId())
                .categoryName(event.categoryName())
                .imageUrl(event.imageUrl())
                .soldCount(soldCount)
                .build();
    }
}