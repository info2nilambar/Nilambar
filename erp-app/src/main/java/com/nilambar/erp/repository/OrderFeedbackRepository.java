package com.nilambar.erp.repository;

import com.nilambar.erp.domain.OrderFeedback;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFeedbackRepository extends JpaRepository<OrderFeedback, Long> {

    Optional<OrderFeedback> findByOrderId(Long orderId);

    List<OrderFeedback> findByOrderIdIn(List<Long> orderIds);
}
