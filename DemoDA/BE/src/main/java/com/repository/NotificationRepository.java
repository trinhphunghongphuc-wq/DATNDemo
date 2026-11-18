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
    // Hide legacy waiting notices for distributors in both the inbox and the unread count.
    String INBOX_SCOPE = """
        from Notification n
        where n.recipientId = :userId and n.recipientRole = :role
          and not (n.recipientRole = com.enums.Role.DISTRIBUTOR and n.eventType = 'RETURN_REQUESTED')
        """;

    @Query("select n " + INBOX_SCOPE)
    Page<Notification> findByRecipientIdAndRecipientRole(@Param("userId") Long recipientId,
                                                         @Param("role") Role role, Pageable pageable);

    @Query("select count(n) " + INBOX_SCOPE + " and n.readAt is null")
    long countByRecipientIdAndRecipientRoleAndReadAtIsNull(@Param("userId") Long recipientId,
                                                         @Param("role") Role role);

    Optional<Notification> findByIdAndRecipientIdAndRecipientRole(Long id, Long recipientId, Role role);

    // Atomic, idempotent: two tabs clicking the same item cannot decrement twice.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Notification n set n.readAt = :now
        where n.recipientId = :userId and n.recipientRole = :role and n.readAt is null
          and exists (
              select 1 from Notification selected
              where selected.id = :id
                and selected.recipientId = n.recipientId and selected.recipientRole = n.recipientRole
                and selected.batchId = n.batchId
                and (n.createdAt < selected.createdAt
                    or (n.createdAt = selected.createdAt and n.id <= selected.id))
          )
        """)
    int markRead(@Param("id") Long id, @Param("userId") Long userId,
                 @Param("role") Role role, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Notification n set n.readAt = :now
        where n.recipientId = :userId and n.recipientRole = :role and n.readAt is null
          and n.batchId = :batchId and n.eventType in ('BATCH_ASSIGNED', 'BATCH_DELIVERED')
        """)
    int markBatchRead(@Param("batchId") Long batchId, @Param("userId") Long userId,
                      @Param("role") Role role, @Param("now") Instant now);
}
