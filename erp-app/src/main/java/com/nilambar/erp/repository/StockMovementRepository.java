package com.nilambar.erp.repository;

import com.nilambar.erp.domain.StockMovement;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByOccurredAtAfter(Instant from);
}
