package com.yarden.productservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ProductEventPublisher {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.product-events}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ProductEvent event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(event.productId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for product {}", event.type(), event.productId(), ex);
                        } else {
                            log.info("Published {} for product {}", event.type(), event.productId());
                        }
                    });
        } catch (Exception ex) {
            log.error("Kafka unavailable, {} for product {} was not published", event.type(), event.productId(), ex);
        }
    }
}