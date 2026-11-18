package com.entity;

import com.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 30)
    private Role recipientRole;
    @Column(name = "batch_id", nullable = false)
    private Long batchId;
    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "read_at")
    private Instant readAt;

    public Notification(Long recipientId, Role recipientRole, Long batchId,
                        String eventType, String title, String message) {
        this.recipientId = recipientId;
        this.recipientRole = recipientRole;
        this.batchId = batchId;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.createdAt = Instant.now();
    }
}
