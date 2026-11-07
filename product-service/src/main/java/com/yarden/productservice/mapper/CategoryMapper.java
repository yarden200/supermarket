package com.yarden.productservice.mapper;

import com.yarden.productservice.dto.CategoryRequest;
import com.yarden.productservice.dto.CategoryResponse;
import com.yarden.productservice.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public Category toEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.name())
                .build();
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }

    public void updateEntity(CategoryRequest request, Category category) {
        category.setName(request.name());
    }
}
