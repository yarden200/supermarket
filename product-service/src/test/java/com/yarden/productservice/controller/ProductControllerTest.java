package com.yarden.productservice.controller;

import com.yarden.productservice.dto.CategoryResponse;
import com.yarden.productservice.dto.ProductResponse;
import com.yarden.productservice.exception.ResourceNotFoundException;
import com.yarden.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for ProductController — only the web layer is loaded, the service is mocked.
 * Security is left out (excluded package + filters off); permissions were checked in Postman.
 */
@WebMvcTest(controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                pattern = "com\\.yarden\\.productservice\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProductService productService;

    @Test
    void getById_whenPresent_returns200() throws Exception {
        ProductResponse response = new ProductResponse(
                1L, "SKU-1", "Milk 3%", "Fresh milk", new BigDecimal("6.90"), 40,
                new CategoryResponse(1L, "Dairy"), null);
        when(productService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Milk 3%"))
                .andExpect(jsonPath("$.category.name").value("Dairy"));
    }

    @Test
    void getById_whenMissing_returns404() throws Exception {
        when(productService.getById(999L)).thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }
}
