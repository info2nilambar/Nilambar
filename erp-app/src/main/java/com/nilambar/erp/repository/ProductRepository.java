package com.nilambar.erp.repository;

import com.nilambar.erp.domain.Product;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select p from Product p
            where p.active = true
              and (:category is null or p.category = :category)
              and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%'))
                                    or lower(p.description) like lower(concat('%', :keyword, '%')))
            """)
    Page<Product> search(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    @Query("select distinct p.category from Product p where p.active = true order by p.category")
    List<String> findCategories();

    Optional<Product> findBySku(String sku);

    List<Product> findByCategory(String category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
