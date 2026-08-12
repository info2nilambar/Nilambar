package com.nilambar.erp.messaging;

import com.nilambar.erp.domain.ProcessedEvent;
import com.nilambar.erp.event.ReturnRequestedEvent;
import com.nilambar.erp.repository.ProcessedEventRepository;
import com.nilambar.erp.service.ReturnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reverse-logistics side of a return: restock and refund happen off the request thread, and
 * redeliveries are ignored by recording the event id.
 */
@Component
public class ReturnEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReturnEventsConsumer.class);
    private static final String CONSUMER_NAME = "return-processing";

    private final ProcessedEventRepository processedEventRepository;
    private final ReturnService returnService;

    public ReturnEventsConsumer(ProcessedEventRepository processedEventRepository, ReturnService returnService) {
        this.processedEventRepository = processedEventRepository;
        this.returnService = returnService;
    }

    @KafkaListener(topics = "${erp.kafka.topics.return-requested}", groupId = "erp-return-processing")
    @Transactional
    public void onReturnRequested(ReturnRequestedEvent event) {
        if (processedEventRepository.existsByEventIdAndConsumer(event.eventId(), CONSUMER_NAME)) {
            log.debug("Skipping already processed event {}", event.eventId());
            return;
        }

        log.info("Return {} for order {}: {} line(s), refund {}, reason {}", event.returnNumber(),
                event.orderNumber(), event.lines().size(), event.refundAmount(), event.reason());

        returnService.settle(event.returnId());
        processedEventRepository.save(new ProcessedEvent(event.eventId(), CONSUMER_NAME));
    }
}
