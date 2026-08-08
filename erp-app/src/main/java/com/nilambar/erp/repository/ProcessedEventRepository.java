package com.nilambar.erp.repository;

import com.nilambar.erp.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventIdAndConsumer(String eventId, String consumer);
}
