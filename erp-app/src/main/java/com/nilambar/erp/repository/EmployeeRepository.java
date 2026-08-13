package com.nilambar.erp.repository;

import com.nilambar.erp.domain.Employee;
import com.nilambar.erp.domain.EmployeeStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByStatusNot(EmployeeStatus status);
}
