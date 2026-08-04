package com.nilambar.erp.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link KafkaTemplate} so that a broker outage never fails a user-facing
 * transaction: publication problems are logged, not propagated.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish event to topic {} key {}: {}", topic, key, ex.getMessage());
                        } else {
                            log.debug("Published event to topic {} key {}", topic, key);
                        }
                    });
        } catch (RuntimeException ex) {
            log.warn("Failed to publish event to topic {} key {}: {}", topic, key, ex.getMessage());
        }
    }
}
