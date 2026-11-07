package com.repository;

import com.entity.RetailUnitPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetailUnitPriceHistoryRepository
        extends JpaRepository<RetailUnitPriceHistory, Long> {

    List<RetailUnitPriceHistory>
    findByRetailUnit_IdOrderByChangedAtDesc(Long retailUnitId);
}