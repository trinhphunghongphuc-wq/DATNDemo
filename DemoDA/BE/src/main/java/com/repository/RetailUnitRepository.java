package com.repository;

import com.entity.RetailUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetailUnitRepository extends JpaRepository<RetailUnit, Long> {

    List<RetailUnit> findByRetailerId(Long retailerId);

    List<RetailUnit> findByBatchIdAndRetailerId(Long batchId, Long retailerId);

    Optional<RetailUnit> findByRetailCode(String retailCode);
}