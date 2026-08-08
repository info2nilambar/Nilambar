package com.nilambar.erp.messaging;

import com.nilambar.erp.service.OrderStatusService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Simulates the fulfilment lifecycle so orders visibly progress towards DELIVERED without a real
 * logistics integration.
 */
@Component
@ConditionalOnProperty(name = "erp.fulfilment.simulate", havingValue = "true", matchIfMissing = true)
public class OrderProgressScheduler {

    private final OrderStatusService orderStatusService;

    public OrderProgressScheduler(OrderStatusService orderStatusService) {
        this.orderStatusService = orderStatusService;
    }

    @Scheduled(fixedDelayString = "${erp.fulfilment.interval-ms:30000}", initialDelayString = "30000")
    public void progressOrders() {
        orderStatusService.advanceOpenOrders();
    }
}
