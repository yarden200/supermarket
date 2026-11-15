package com.yarden.imageservice.service;

import com.yarden.imageservice.dto.ImageUploadResponse;
import com.yarden.imageservice.exception.ImageNotFoundException;
import com.yarden.imageservice.exception.InvalidImageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ImageService {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private static final Pattern FILE_NAME = Pattern.compile("[0-9a-f\\-]{36}\\.(jpg|png|webp)");

    @Autowired
    private S3StorageService storage;

    public ImageUploadResponse uploadProductImage(Long productId, MultipartFile file) {
        String extension = validate(file);
        String key = productKey(productId, UUID.randomUUID() + "." + extension);

        try (InputStream in = file.getInputStream()) {
            storage.upload(key, file.getContentType(), in, file.getSize());
        } catch (IOException e) {
            throw new InvalidImageException("Could not read the uploaded file");
        }

        return new ImageUploadResponse(key, "/api/images/" + key);
    }

    public void deleteProductImage(Long productId, String fileName) {
        if (!FILE_NAME.matcher(fileName).matches()) {
            throw new ImageNotFoundException(fileName);
        }
        String key = productKey(productId, fileName);
        if (!storage.exists(key)) {
            throw new ImageNotFoundException(key);
        }
        storage.delete(key);
    }

    public URI presignedProductImageUrl(Long productId, String fileName) {
        if (!FILE_NAME.matcher(fileName).matches()) {
            throw new ImageNotFoundException(fileName);
        }
        String key = productKey(productId, fileName);

        if (!storage.exists(key)) {
            throw new ImageNotFoundException(key);
        }
        return storage.presignedGetUrl(key);
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Image file is required");
        }
        String contentType = file.getContentType();
        String extension = contentType == null ? null : ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new InvalidImageException("Only JPEG, PNG or WEBP images are allowed");
        }
        return extension;
    }

    private String productKey(Long productId, String fileName) {
        return "products/" + productId + "/" + fileName;
    }
}