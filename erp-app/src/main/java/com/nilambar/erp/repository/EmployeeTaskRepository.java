package com.nilambar.erp.repository;

import com.nilambar.erp.domain.EmployeeTask;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeTaskRepository extends JpaRepository<EmployeeTask, Long> {

    List<EmployeeTask> findByDueOnAfter(LocalDate date);
}
