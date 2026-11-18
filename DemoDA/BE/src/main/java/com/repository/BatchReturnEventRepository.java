package com.repository;

import com.entity.BatchReturnEvent;
import com.entity.BatchReturnEvent.Type;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchReturnEventRepository extends JpaRepository<BatchReturnEvent, Long> {
    Optional<BatchReturnEvent> findFirstByBatchIdAndEventType(Long batchId, Type type);

    List<BatchReturnEvent> findByBatchIdAndEventTypeOrderById(Long batchId, Type type);

    Slice<BatchReturnEvent> findByBatchIdAndEventTypeOrderByIdDesc(Long batchId, Type type, Pageable page);

    List<BatchReturnEvent> findByBatchIdAndEventTypeNotOrderById(Long batchId, Type type);
}
