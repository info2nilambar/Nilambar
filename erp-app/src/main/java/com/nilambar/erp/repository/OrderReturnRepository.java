package com.nilambar.erp.repository;

import com.nilambar.erp.domain.OrderReturn;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {

    List<OrderReturn> findByOrderIdOrderByIdDesc(Long orderId);

    Optional<OrderReturn> findByReturnNumber(String returnNumber);
}
