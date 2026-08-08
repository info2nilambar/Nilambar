package com.nilambar.erp.service;

import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.OrderFeedback;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.repository.OrderFeedbackRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final OrderFeedbackRepository feedbackRepository;
    private final OrderService orderService;
    private final Clock clock;

    public FeedbackService(OrderFeedbackRepository feedbackRepository, OrderService orderService, Clock clock) {
        this.feedbackRepository = feedbackRepository;
        this.orderService = orderService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<OrderFeedback> forOrder(Long orderId) {
        return feedbackRepository.findByOrderId(orderId);
    }

    /**
     * Feedback is accepted once per order, and only after the order has actually reached the
     * customer.
     */
    @Transactional
    public OrderFeedback submit(User user, Long orderId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("Rating must be between 1 and 5.");
        }
        CustomerOrder order = orderService.require(user, orderId);
        if (!isComplete(order.getStatus())) {
            throw new BusinessException("You can review this order once it has been delivered or picked up.");
        }
        if (feedbackRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessException("You have already reviewed this order.");
        }

        OrderFeedback feedback = new OrderFeedback();
        feedback.setOrder(order);
        feedback.setRating(rating);
        feedback.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        feedback.setCreatedAt(clock.instant());
        return feedbackRepository.save(feedback);
    }

    private boolean isComplete(OrderStatus status) {
        return status == OrderStatus.DELIVERED || status == OrderStatus.PICKED_UP;
    }
}
