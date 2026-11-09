package com.yarden.productservice.mapper;

import com.yarden.productservice.dto.ProductRequest;
import com.yarden.productservice.dto.ProductResponse;
import com.yarden.productservice.dto.StockItemResponse;
import com.yarden.productservice.model.Category;
import com.yarden.productservice.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    @Autowired
    private CategoryMapper categoryMapper;

    public Product toEntity(ProductRequest request,Category category) {
        return Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .price(request.price().setScale(2))
                .stockQuantity(request.stockQuantity())
                .category(category)
                .build();
    }

    public ProductResponse toResponse(Product product){
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                categoryMapper.toResponse(product.getCategory()),
                product.getImageUrl()
        );
    }

    public void updateEntity(ProductRequest request, Product product, Category category) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price().setScale(2));
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(category);
    }

    public StockItemResponse toStockResponse(Product product) {
        return new StockItemResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity()
        );
    }
}
