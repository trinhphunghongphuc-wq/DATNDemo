package com.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "batch_return_cases")
@Getter
@Setter
@NoArgsConstructor
public class BatchReturnCase {
    public enum Status {
        REQUESTED, APPROVED, IN_TRANSIT, QUARANTINED, INSPECTED, DECIDED, CLOSED
    }

    @Id
    private Long batchId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "numeric")
    private BigDecimal requestedWeight;

    @Column(columnDefinition = "numeric")
    private BigDecimal receivedWeight;

    private Long vehicleId;
    private String warehouseName;
    private String warehouseAddress;
    private Instant createdAt;
    private Instant updatedAt;

    public BatchReturnCase(Long batchId, BigDecimal requestedWeight, Instant now) {
        this.batchId = batchId;
        this.requestedWeight = requestedWeight;
        this.createdAt = now;
        this.updatedAt = now;
        this.status = Status.REQUESTED;
    }
}
