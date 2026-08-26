package com.repository;

import com.entity.Batch;
import com.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    long countByChainTxHashIsNotNull();

    long countByChainTxHashIsNull();

    List<Batch> findTop5ByOrderByCreatedAtDesc();

    long countByMerkleRootIsNotNull();

    long countByMerkleRootIsNull();


    List<Batch> findTop10ByChainTxHashIsNotNullOrderByCreatedAtDesc();

    List<Batch> findTop10ByChainTxHashIsNullOrderByCreatedAtDesc();

    Optional<Batch> findByBatchCode(String batchCode);

    //Profession Producer
    List<Batch> findByCreatedByUsernameOrderByCreatedAtDesc(String username);
    Optional<Batch> findByIdAndCreatedByUsername(Long id, String username);

    //Profession Distributor
    List<Batch> findByDistributorId(Long distributorId);
    List<Batch> findByDistributorIdAndStatus(Long distributorId, BatchStatus status);
    Optional<Batch> findByIdAndDistributorId(Long id, Long distributorId);

    // Profession Retailer
    List<Batch> findByRetailerId(Long retailerId);
    List<Batch> findByRetailerIdAndStatus(Long retailerId, BatchStatus status);
    Optional<Batch> findByIdAndRetailerId(Long id, Long retailerId);

}