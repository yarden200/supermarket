package com.yarden.searchservice.listener;

import com.yarden.searchservice.document.ProductDocument;
import com.yarden.searchservice.event.ProductEvent;
import com.yarden.searchservice.mapper.ProductDocumentMapper;
import com.yarden.searchservice.repository.ProductSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductEventsListener {

    @Autowired
    private ProductSearchRepository repository;

    @Autowired
    private ProductDocumentMapper mapper;

    @KafkaListener(
            topics = "${app.kafka.topics.product-events}",
            properties = "spring.json.value.default.type=com.yarden.searchservice.event.ProductEvent")
    public void onProductEvent(ProductEvent event) {
        String id = String.valueOf(event.productId());
        switch (event.type()) {
            case CREATED, UPDATED -> {
                Long soldCount = repository.findById(id)
                        .map(ProductDocument::getSoldCount)
                        .orElse(0L);
                repository.save(mapper.toDocument(event, soldCount));
                log.info("Indexed product {} ({})", event.productId(), event.type());
            }
            case DELETED -> {
                repository.deleteById(id);
                log.info("Removed product {} from index", event.productId());
            }
        }
    }
}
