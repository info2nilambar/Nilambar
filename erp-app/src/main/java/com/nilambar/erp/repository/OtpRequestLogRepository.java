package com.nilambar.erp.repository;

import com.nilambar.erp.domain.OtpRequestLog;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRequestLogRepository extends JpaRepository<OtpRequestLog, Long> {

    long countByMobileAndRequestedAtAfter(String mobile, Instant since);
}
