package com.nilambar.erp.messaging;

import com.nilambar.erp.domain.ProcessedEvent;
import com.nilambar.erp.event.OrderPlacedEvent;
import com.nilambar.erp.repository.ProcessedEventRepository;
import com.nilambar.erp.service.OrderStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventory / notification side of order placement. Redeliveries are ignored by recording the
 * event id, so the status machine is never advanced twice for the same event.
 */
@Component
public class OrderEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);
    private static final String CONSUMER_NAME = "order-fulfilment";

    private final ProcessedEventRepository processedEventRepository;
    private final OrderStatusService orderStatusService;

    public OrderEventsConsumer(ProcessedEventRepository processedEventRepository,
                               OrderStatusService orderStatusService) {
        this.processedEventRepository = processedEventRepository;
        this.orderStatusService = orderStatusService;
    }

    @KafkaListener(topics = "${erp.kafka.topics.order-placed}", groupId = "erp-order-fulfilment")
    @Transactional
    public void onOrderPlaced(OrderPlacedEvent event) {
        if (processedEventRepository.existsByEventIdAndConsumer(event.eventId(), CONSUMER_NAME)) {
            log.debug("Skipping already processed event {}", event.eventId());
            return;
        }

        log.info("Order {} received: {} line(s), total {}, fulfilment {} ({} km)",
                event.orderNumber(), event.lines().size(), event.total(), event.fulfilmentType(), event.distanceKm());

        orderStatusService.advance(event.orderId());
        processedEventRepository.save(new ProcessedEvent(event.eventId(), CONSUMER_NAME));
    }
}
