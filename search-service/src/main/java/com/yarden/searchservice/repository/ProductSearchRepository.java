package com.yarden.searchservice.repository;

import com.yarden.searchservice.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    @Query("""
            {
              "multi_match": {
                "query": "?0",
                "fields": ["name^3", "description"],
                "fuzziness": "AUTO"
              }
            }
            """)
    Page<ProductDocument> searchByText(String text, Pageable pageable);

    Page<ProductDocument> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("""
            {
              "bool": {
                "must": {
                  "multi_match": {
                    "query": "?0",
                    "fields": ["name^3", "description"],
                    "fuzziness": "AUTO"
                  }
                },
                "filter": {
                  "term": { "categoryId": ?1 }
                }
              }
            }
            """)
    Page<ProductDocument> searchByTextInCategory(String text, Long categoryId, Pageable pageable);
}
