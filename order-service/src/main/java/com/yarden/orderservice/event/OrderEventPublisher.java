package com.yarden.orderservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OrderEventPublisher {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-events}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(OrderEvent event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(event.orderId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for order {}", event.type(), event.orderId(), ex);
                        } else {
                            log.info("Published {} for order {}", event.type(), event.orderId());
                        }
                    });
        } catch (Exception ex) {
            log.error("Kafka unavailable, {} for order {} was not published", event.type(), event.orderId(), ex);
        }
    }
}