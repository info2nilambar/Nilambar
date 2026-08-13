package com.nilambar.erp.repository;

import com.nilambar.erp.domain.PurchaseOrder;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByOrderedAtAfter(Instant from);
}
