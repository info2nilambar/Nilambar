package com.nilambar.erp.repository;

import com.nilambar.erp.domain.AttendanceRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByWorkDateBetween(LocalDate from, LocalDate to);
}
