package com.entity;

import com.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "batch_return_events")
@Getter
@NoArgsConstructor
public class BatchReturnEvent {
    public enum Type {
        REJECTED, APPROVED, TRIP_STARTED, TELEMETRY, RECEIVED, INSPECTED, DECIDED, EXECUTED, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long batchId;
    private Long actorId;

    @Enumerated(EnumType.STRING)
    private Role actorRole;

    @Enumerated(EnumType.STRING)
    private Type eventType;

    private Instant occurredAt;

    @Column(columnDefinition = "text")
    private String details;

    public BatchReturnEvent(Long batchId, Long actorId, Role role, Type type, Instant time, String details) {
        this.batchId = batchId;
        this.actorId = actorId;
        this.actorRole = role;
        this.eventType = type;
        this.occurredAt = time;
        this.details = details;
    }
}
