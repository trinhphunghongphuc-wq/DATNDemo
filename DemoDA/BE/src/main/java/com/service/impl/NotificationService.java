package com.service.impl;

import com.dto.notification.*;
import com.entity.Batch;
import com.entity.Notification;
import com.enums.Role;
import com.repository.NotificationRepository;
import com.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.MANDATORY)
    public void batchAssigned(Batch batch) {
        create(batch.getDistributorId(), Role.DISTRIBUTOR, batch, "BATCH_ASSIGNED",
                "Bạn có lô hàng mới", "Lô " + batch.getBatchCode() + " vừa được phân công cho bạn.");
        create(batch.getRetailerId(), Role.RETAILER, batch, "BATCH_ASSIGNED",
                "Bạn có lô hàng được phân công",
                "Lô " + batch.getBatchCode() + " được phân công cho bạn. Đang chờ nhà phân phối giao hàng.");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void batchDelivered(Batch batch) {
        create(batch.getRetailerId(), Role.RETAILER, batch, "BATCH_DELIVERED",
                "Có lô hàng chờ nhận", "Lô " + batch.getBatchCode() + " đã được giao đến bạn.");
    }

    private void create(Long userId, Role role, Batch batch, String type, String title, String message) {
        if (userId == null) return;
        notifications.save(new Notification(userId, role, batch.getId(), type, title, message));
        // Persist with the business operation; SSE is emitted only AFTER_COMMIT.
        events.publishEvent(new NotificationChanged(userId));
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse list(CustomUserDetails user, int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page >= 0; size từ 1 đến 50");
        }
        var result = notifications.findByRecipientIdAndRecipientRole(user.getId(), user.getRole(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new NotificationPageResponse(result.map(NotificationResponse::from).getContent(),
                unreadCount(user), page, result.hasNext());
    }

    @Transactional(readOnly = true)
    public long unreadCount(CustomUserDetails user) {
        return notifications.countByRecipientIdAndRecipientRoleAndReadAtIsNull(user.getId(), user.getRole());
    }

    @Transactional
    public NotificationResponse read(CustomUserDetails user, Long id) {
        notifications.findByIdAndRecipientIdAndRecipientRole(id, user.getId(), user.getRole())
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền đọc thông báo này."));
        if (notifications.markRead(id, user.getId(), user.getRole(), Instant.now()) > 0) {
            events.publishEvent(new NotificationChanged(user.getId()));
        }
        return NotificationResponse.from(notifications
                .findByIdAndRecipientIdAndRecipientRole(id, user.getId(), user.getRole()).orElseThrow());
    }
}
