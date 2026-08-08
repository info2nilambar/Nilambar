package com.nilambar.erp.repository;

import com.nilambar.erp.domain.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByDefaultAddressDescIdAsc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    Optional<Address> findByUserIdAndDefaultAddressTrue(Long userId);

    @Modifying
    @Query("update Address a set a.defaultAddress = false where a.user.id = :userId")
    void clearDefaultFor(@Param("userId") Long userId);
}
