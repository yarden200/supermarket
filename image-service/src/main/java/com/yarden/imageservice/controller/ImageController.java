package com.yarden.imageservice.controller;

import com.yarden.imageservice.dto.ImageUploadResponse;
import com.yarden.imageservice.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping(path = "/products/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImageUploadResponse> uploadProductImage(@PathVariable Long productId,
                                                                  @RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = imageService.uploadProductImage(productId, file);
        return ResponseEntity.created(URI.create(response.url())).body(response);
    }

    @GetMapping("/products/{productId}/{fileName}")
    public ResponseEntity<Void> getProductImage(@PathVariable Long productId,
                                                @PathVariable String fileName) {
        URI url = imageService.presignedProductImageUrl(productId, fileName);
        return ResponseEntity.status(HttpStatus.FOUND).location(url).build();
    }

    @DeleteMapping("/products/{productId}/{fileName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductImage(@PathVariable Long productId,
                                                   @PathVariable String fileName) {
        imageService.deleteProductImage(productId, fileName);
        return ResponseEntity.noContent().build();
    }
}
