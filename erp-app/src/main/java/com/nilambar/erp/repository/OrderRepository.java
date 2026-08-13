package com.nilambar.erp.repository;

import com.nilambar.erp.domain.CustomerOrder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByUserIdOrderByIdDesc(Long userId);

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);

    List<CustomerOrder> findByCreatedAtAfter(Instant from);
}
