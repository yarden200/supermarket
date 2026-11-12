package com.yarden.searchservice.listener;

import com.yarden.searchservice.event.OrderEvent;
import com.yarden.searchservice.event.OrderEventItem;
import com.yarden.searchservice.event.OrderEventType;
import com.yarden.searchservice.repository.ProductSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventsListener {

    @Autowired
    private ProductSearchRepository repository;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            properties = "spring.json.value.default.type=com.yarden.searchservice.event.OrderEvent")
    public void onOrderEvent(OrderEvent event) {
        if (event.type() != OrderEventType.CREATED) {
            log.info("Ignoring {} for order {}", event.type(), event.orderId());
            return;
        }
        for (OrderEventItem item : event.items()) {
            repository.findById(String.valueOf(item.productId())).ifPresent(document -> {
                long current = document.getSoldCount() != null ? document.getSoldCount() : 0L;
                document.setSoldCount(current + item.quantity());
                repository.save(document);
            });
        }
        log.info("Updated sold counts for order {}", event.orderId());
    }
}