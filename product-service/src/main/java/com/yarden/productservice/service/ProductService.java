package com.yarden.productservice.service;

import com.yarden.productservice.dto.*;
import com.yarden.productservice.exception.DuplicateResourceException;
import com.yarden.productservice.exception.InsufficientStockException;
import com.yarden.productservice.exception.ResourceNotFoundException;
import com.yarden.productservice.mapper.ProductMapper;
import com.yarden.productservice.model.Category;
import com.yarden.productservice.model.Product;
import com.yarden.productservice.repository.CategoryRepository;
import com.yarden.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductRepository productRepository;



    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product", "sku", request.sku());
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        Product product = productMapper.toEntity(request, category);
        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new DuplicateResourceException("Product", "sku", request.sku());
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        productMapper.updateEntity(request, product, category);

        return productMapper.toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public List<StockItemResponse> decreaseStock(StockDecreaseRequest request) {
        Map<Long, Integer> requested = request.items().stream()
                .collect(Collectors.toMap(
                        StockItemRequest::productId,
                        StockItemRequest::quantity,
                        Integer::sum,
                        TreeMap::new));

        List<Product> products = productRepository.findAllByIdForUpdate(requested.keySet());

        if (products.size() < requested.size()) {
            Set<Long> found = products.stream().map(Product::getId).collect(Collectors.toSet());
            Long missingId = requested.keySet().stream()
                    .filter(id -> !found.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new ResourceNotFoundException("Product", missingId);
        }

        List<InsufficientStockException.StockShortage> shortages = new ArrayList<>();
        for (Product product : products) {
            int quantity = requested.get(product.getId());
            if (product.getStockQuantity() < quantity) {
                shortages.add(new InsufficientStockException.StockShortage(
                        product.getId(), quantity, product.getStockQuantity()));
            }
        }
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }

        for (Product product : products) {
            int quantity = requested.get(product.getId());
            product.setStockQuantity(product.getStockQuantity() - quantity);
        }

        return products.stream()
                .map(productMapper::toStockResponse)
                .toList();
    }
}
