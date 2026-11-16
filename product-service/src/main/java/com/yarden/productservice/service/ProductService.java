package com.yarden.productservice.service;

import com.yarden.productservice.client.ImageClient;
import com.yarden.productservice.dto.*;
import com.yarden.productservice.event.ProductEvent;
import com.yarden.productservice.event.ProductEventType;
import com.yarden.productservice.exception.*;
import com.yarden.productservice.mapper.ProductMapper;
import com.yarden.productservice.model.Category;
import com.yarden.productservice.model.Product;
import com.yarden.productservice.repository.CategoryRepository;
import com.yarden.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductService {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ImageClient imageClient;
    @Autowired
    private TransactionTemplate transactionTemplate;



    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Long categoryId, Pageable pageable) {
        if (categoryId == null) {
            return productRepository.findAll(pageable).map(productMapper::toResponse);
        }
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
        return productRepository.findByCategoryId(categoryId, pageable).map(productMapper::toResponse);
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
        eventPublisher.publishEvent(productMapper.toEvent(saved, ProductEventType.CREATED));

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
        eventPublisher.publishEvent(productMapper.toEvent(product, ProductEventType.UPDATED));

        return productMapper.toResponse(product);
    }

    public void delete(Long id) {
        String imagePath = transactionTemplate.execute(status -> {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", id));
            String path = product.getImageUrl();
            productRepository.delete(product);
            eventPublisher.publishEvent(ProductEvent.deleted(id));
            return path;
        });

        if (imagePath != null) {
            deleteImageFile(imagePath, id);
        }
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
            eventPublisher.publishEvent(productMapper.toEvent(product, ProductEventType.UPDATED));
        }

        return products.stream()
                .map(productMapper::toStockResponse)
                .toList();
    }

    public ProductResponse uploadImage(Long id, MultipartFile file) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }

        ImageUploadResponse uploaded = imageClient.uploadProductImage(id, file);

        return transactionTemplate.execute(status -> {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", id));
            product.setImageUrl(uploaded.url());
            eventPublisher.publishEvent(productMapper.toEvent(product, ProductEventType.UPDATED));
            return productMapper.toResponse(product);
        });
    }

    public void deleteImage(Long id) {
        String imagePath = transactionTemplate.execute(status -> {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", id));
            if (product.getImageUrl() == null) {
                throw new ResourceNotFoundException("Product image", id);
            }
            String path = product.getImageUrl();
            product.setImageUrl(null);
            eventPublisher.publishEvent(productMapper.toEvent(product, ProductEventType.UPDATED));
            return path;
        });

        deleteImageFile(imagePath, id);
    }

    private void deleteImageFile(String imagePath, Long productId) {
        try {
            imageClient.deleteImage(imagePath);
        } catch (ImageServiceException | ImageServiceUnavailableException e) {
            log.warn("Image {} of product {} was not deleted from storage: {}", imagePath, productId, e.getMessage());
        }
    }
}
