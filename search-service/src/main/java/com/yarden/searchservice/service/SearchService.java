package com.yarden.searchservice.service;

import com.yarden.searchservice.document.ProductDocument;
import com.yarden.searchservice.repository.ProductSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SearchService {

    @Autowired
    private ProductSearchRepository repository;

    public Page<ProductDocument> search(String text, Long categoryId, Pageable pageable) {
        boolean hasText = text != null && !text.isBlank();

        if (hasText && categoryId != null) {
            return repository.searchByTextInCategory(text, categoryId, pageable);
        }
        if (hasText) {
            return repository.searchByText(text, pageable);
        }
        if (categoryId != null) {
            return repository.findByCategoryId(categoryId, pageable);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a search text (q) and/or a categoryId");
    }
}
