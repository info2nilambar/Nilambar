package com.nilambar.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.OrderFeedback;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.repository.OrderFeedbackRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeedbackServiceTest {

    private OrderFeedbackRepository feedbackRepository;
    private FeedbackService feedbackService;
    private CustomerOrder order;
    private User user;

    @BeforeEach
    void setUp() {
        feedbackRepository = mock(OrderFeedbackRepository.class);
        OrderService orderService = mock(OrderService.class);

        user = new User();
        user.setId(1L);

        order = new CustomerOrder();
        order.setId(9L);
        order.setStatus(OrderStatus.DELIVERED);

        when(orderService.require(user, 9L)).thenReturn(order);
        when(feedbackRepository.findByOrderId(9L)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(OrderFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        feedbackService = new FeedbackService(feedbackRepository, orderService,
                Clock.fixed(Instant.parse("2026-02-01T09:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void deliveredOrderAcceptsRatingAndComment() {
        OrderFeedback feedback = feedbackService.submit(user, 9L, 5, "  Fast delivery  ");

        assertThat(feedback.getRating()).isEqualTo(5);
        assertThat(feedback.getComment()).isEqualTo("Fast delivery");
        assertThat(feedback.getOrder()).isSameAs(order);
    }

    @Test
    void blankCommentIsStoredAsNull() {
        assertThat(feedbackService.submit(user, 9L, 4, "   ").getComment()).isNull();
    }

    @Test
    void ratingOutsideOneToFiveIsRejected() {
        assertThatThrownBy(() -> feedbackService.submit(user, 9L, 6, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between 1 and 5");
    }

    @Test
    void orderStillInFlightCannotBeReviewed() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);

        assertThatThrownBy(() -> feedbackService.submit(user, 9L, 5, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("once it has been delivered");
    }

    @Test
    void secondReviewForTheSameOrderIsRejected() {
        when(feedbackRepository.findByOrderId(9L)).thenReturn(Optional.of(new OrderFeedback()));

        assertThatThrownBy(() -> feedbackService.submit(user, 9L, 5, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void pickedUpOrderCanBeReviewed() {
        order.setStatus(OrderStatus.PICKED_UP);

        assertThat(feedbackService.submit(user, 9L, 3, null).getRating()).isEqualTo(3);
    }
}
