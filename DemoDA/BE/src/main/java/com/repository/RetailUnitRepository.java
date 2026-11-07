package com.repository;

import com.entity.RetailUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RetailUnitRepository extends JpaRepository<RetailUnit, Long> {

    List<RetailUnit> findByRetailerId(Long retailerId);

    List<RetailUnit> findByBatchIdAndRetailerId(Long batchId, Long retailerId);

    Optional<RetailUnit> findByRetailCode(String retailCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from RetailUnit u where u.id = :id")
    Optional<RetailUnit> findByIdForPriceUpdate(@Param("id") Long id);
}