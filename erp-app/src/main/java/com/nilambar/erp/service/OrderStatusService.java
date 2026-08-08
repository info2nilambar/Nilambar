package com.nilambar.erp.service;

import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.repository.OrderRepository;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);
    private static final Set<OrderStatus> OPEN = EnumSet.of(
            OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.READY_FOR_PICKUP);

    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderStatusService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Transactional
    public void advance(Long orderId) {
        orderRepository.findById(orderId).ifPresent(this::advanceOne);
    }

    @Transactional
    public void advanceOpenOrders() {
        List<CustomerOrder> open = orderRepository.findAll().stream()
                .filter(order -> OPEN.contains(order.getStatus()))
                .toList();
        open.forEach(this::advanceOne);
    }

    private void advanceOne(CustomerOrder order) {
        OrderStatus next = order.getStatus().next(order.getFulfilmentType());
        if (next == order.getStatus()) {
            return;
        }
        log.info("Order {} status {} -> {}", order.getOrderNumber(), order.getStatus(), next);
        order.setStatus(next);
        order.setUpdatedAt(clock.instant());
    }
}
