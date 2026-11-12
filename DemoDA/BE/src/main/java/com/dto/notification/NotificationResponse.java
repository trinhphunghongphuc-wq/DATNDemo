package com.dto.notification;

import com.entity.Notification;
import com.enums.Role;
import java.time.Instant;

public record NotificationResponse(Long id, Long batchId, String eventType,
        String title, String message, Instant createdAt, Instant readAt, String targetUrl) {
    public static NotificationResponse from(Notification n) {
        String path = n.getRecipientRole() == Role.DISTRIBUTOR
                ? "/distributor/batches/" + n.getBatchId()
                : "/retailer/batches/" + n.getBatchId() + "/records";
        return new NotificationResponse(n.getId(), n.getBatchId(), n.getEventType(),
                n.getTitle(), n.getMessage(), n.getCreatedAt(), n.getReadAt(), path);
    }
}
