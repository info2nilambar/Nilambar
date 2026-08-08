package com.nilambar.erp.repository;

import com.nilambar.erp.domain.OtpCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByMobileAndConsumedFalseOrderByIdDesc(String mobile);

    @Modifying
    @Query("update OtpCode o set o.consumed = true where o.mobile = :mobile and o.consumed = false")
    void consumeAllFor(@Param("mobile") String mobile);
}
