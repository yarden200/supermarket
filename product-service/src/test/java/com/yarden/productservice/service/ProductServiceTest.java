package com.yarden.productservice.service;

import com.yarden.productservice.client.ImageClient;
import com.yarden.productservice.dto.ProductRequest;
import com.yarden.productservice.dto.ProductResponse;
import com.yarden.productservice.event.ProductEvent;
import com.yarden.productservice.event.ProductEventType;
import com.yarden.productservice.exception.DuplicateResourceException;
import com.yarden.productservice.exception.ResourceNotFoundException;
import com.yarden.productservice.mapper.ProductMapper;
import com.yarden.productservice.model.Category;
import com.yarden.productservice.model.Product;
import com.yarden.productservice.repository.CategoryRepository;
import com.yarden.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for ProductService — Mockito only, no Spring.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ProductRepository productRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ImageClient imageClient;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private ProductService productService;

    private final ProductRequest request = new ProductRequest(
            "SKU-1", "Milk 3%", "Fresh milk", new BigDecimal("6.90"), 40, 1L);

    @Test
    void create_savesAndPublishesEvent() {
        Category category = new Category();
        Product product = new Product();
        Product saved = Product.builder().id(99L).build();
        ProductEvent event = mock(ProductEvent.class);
        ProductResponse response = mock(ProductResponse.class);

        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productMapper.toEntity(request, category)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(saved);
        when(productMapper.toEvent(saved, ProductEventType.CREATED)).thenReturn(event);
        when(productMapper.toResponse(saved)).thenReturn(response);

        ProductResponse result = productService.create(request);

        assertThat(result).isSameAs(response);
        verify(eventPublisher).publishEvent(event);
    }

    @Test
    void create_withDuplicateSku_throws() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void getById_whenMissing_throws() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_whenPresent_returnsIt() {
        Product product = Product.builder().id(1L).build();
        ProductResponse response = mock(ProductResponse.class);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.getById(1L);

        assertThat(result).isSameAs(response);
    }
}