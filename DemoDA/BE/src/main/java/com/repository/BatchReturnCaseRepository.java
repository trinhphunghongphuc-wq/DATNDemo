package com.repository;

import com.entity.BatchReturnCase;
import com.entity.BatchReturnCase.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchReturnCaseRepository extends JpaRepository<BatchReturnCase, Long> {
    boolean existsByVehicleIdAndStatus(Long vehicleId, Status status);

    @Query("""
            select c from BatchReturnCase c, Batch b
            where c.batchId = b.id
              and (:role = 'PRODUCER' and b.createdBy.id = :userId
                   or :role = 'DISTRIBUTOR' and b.distributorId = :userId
                   or :role = 'RETAILER' and b.retailerId = :userId)
            order by c.updatedAt desc, c.batchId desc
            """)
    Slice<BatchReturnCase> findForParty(@Param("userId") Long userId,
                                      @Param("role") String role, Pageable pageable);
}
