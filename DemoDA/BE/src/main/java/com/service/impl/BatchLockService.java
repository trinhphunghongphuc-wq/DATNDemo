package com.service.impl;

import com.entity.Batch;
import com.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class BatchLockService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public Batch lock(Long batchId) {
        if (batchId == null) {
            throw new IllegalArgumentException("batchId is required");
        }

        if (TransactionSynchronizationManager
                .isCurrentTransactionReadOnly()) {
            throw new IllegalStateException(
                    "Batch write lock requires a writable transaction"
            );
        }

        Batch batch = entityManager.find(Batch.class, batchId);

        if (batch == null) {
            throw new ResourceNotFoundException(
                    "Batch not found with id: " + batchId
            );
        }

        LockModeType lockMode = entityManager.getLockMode(batch);

        if (lockMode != LockModeType.PESSIMISTIC_WRITE
                && lockMode != LockModeType.PESSIMISTIC_FORCE_INCREMENT) {
            /*
             * Khóa và nạp lại dữ liệu hiện tại.
             * Đối tượng có thể đã được đọc ở bước kiểm tra quyền.
             */
            entityManager.refresh(
                    batch,
                    LockModeType.PESSIMISTIC_WRITE
            );
        }

        return batch;
    }
}