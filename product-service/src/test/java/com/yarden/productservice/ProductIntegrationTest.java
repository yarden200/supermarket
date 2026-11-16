package com.yarden.productservice;

import com.yarden.productservice.model.Category;
import com.yarden.productservice.model.Product;
import com.yarden.productservice.repository.CategoryRepository;
import com.yarden.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test — the whole service runs (controller, service, repositories, mapper)
 * against an in-memory H2 database (src/test/resources/application.properties).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;

    @Test
    void getById_returnsProductFromDatabase() throws Exception {
        Category category = categoryRepository.save(Category.builder().name("Dairy").build());
        Product product = productRepository.save(Product.builder()
                .sku("SKU-1")
                .name("Milk 3%")
                .description("Fresh milk")
                .price(new BigDecimal("6.90"))
                .stockQuantity(40)
                .category(category)
                .build());

        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Milk 3%"))
                .andExpect(jsonPath("$.category.name").value("Dairy"));
    }
}