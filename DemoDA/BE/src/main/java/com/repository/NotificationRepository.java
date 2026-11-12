package com.repository;

import com.entity.Notification;
import com.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdAndRecipientRole(Long recipientId, Role role, Pageable pageable);
    long countByRecipientIdAndRecipientRoleAndReadAtIsNull(Long recipientId, Role role);
    Optional<Notification> findByIdAndRecipientIdAndRecipientRole(Long id, Long recipientId, Role role);

    // Atomic, idempotent: two tabs clicking the same item cannot decrement twice.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Notification n set n.readAt = :now
        where n.id = :id and n.recipientId = :userId
          and n.recipientRole = :role and n.readAt is null
        """)
    int markRead(@Param("id") Long id, @Param("userId") Long userId,
                 @Param("role") Role role, @Param("now") Instant now);
}
    