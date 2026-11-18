package com.repository;

import com.entity.Notification;
import com.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:notification-read;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class}, mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class NotificationReadTest {
    private static final long USER = 1L;
    private static final long BATCH = 20L;
    private static final Role ROLE = Role.RETAILER;
    private static final Instant ASSIGNED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant DELIVERED_AT = ASSIGNED_AT.plusSeconds(60);
    private static final Instant READ_AT = DELIVERED_AT.plusSeconds(60);

    @Autowired
    private NotificationRepository notifications;

    @Test
    void clickingOlderItemLeavesNewerItemUnreadEvenWithIdenticalTimestamps() {
        var assigned = save(USER, ROLE, BATCH, "BATCH_ASSIGNED", ASSIGNED_AT);
        var delivered = save(USER, ROLE, BATCH, "BATCH_DELIVERED", ASSIGNED_AT);

        assertEquals(1, notifications.markRead(assigned.getId(), USER, ROLE, READ_AT));
        assertEquals(READ_AT, readAt(assigned));
        assertNull(readAt(delivered));
        assertEquals(1, notifications.countByRecipientIdAndRecipientRoleAndReadAtIsNull(USER, ROLE));

        assertEquals(1, notifications.markRead(delivered.getId(), USER, ROLE, READ_AT));
        assertEquals(0, notifications.markRead(delivered.getId(), USER, ROLE, READ_AT));
    }

    @Test
    void clickingNewerItemMarksOlderItemsForTheSameRecipientAndBatch() {
        var delivered = save(USER, ROLE, BATCH, "BATCH_DELIVERED", DELIVERED_AT);
        var assigned = save(USER, ROLE, BATCH, "BATCH_ASSIGNED", ASSIGNED_AT);
        var otherBatch = save(USER, ROLE, BATCH + 1, "BATCH_ASSIGNED", ASSIGNED_AT);
        var otherUser = save(USER + 1, ROLE, BATCH, "BATCH_ASSIGNED", ASSIGNED_AT);
        var otherRole = save(USER, Role.DISTRIBUTOR, BATCH, "BATCH_ASSIGNED", ASSIGNED_AT);

        assertEquals(0, notifications.markRead(delivered.getId(), USER + 1, ROLE, READ_AT));
        assertEquals(2, notifications.markRead(delivered.getId(), USER, ROLE, READ_AT));
        assertEquals(READ_AT, readAt(assigned));
        assertEquals(READ_AT, readAt(delivered));
        assertNull(readAt(otherBatch));
        assertNull(readAt(otherUser));
        assertNull(readAt(otherRole));
    }

    @Test
    void openingBatchDetailMarksAssignmentAndDeliveryButLeavesReturnNoticeUnread() {
        var assigned = save(USER, ROLE, BATCH, "BATCH_ASSIGNED", ASSIGNED_AT);
        var delivered = save(USER, ROLE, BATCH, "BATCH_DELIVERED", DELIVERED_AT);
        var returnNotice = save(USER, ROLE, BATCH, "RETURN_APPROVED", DELIVERED_AT);
        var otherBatch = save(USER, ROLE, BATCH + 1, "BATCH_ASSIGNED", ASSIGNED_AT);
        var distributor = save(USER + 1, Role.DISTRIBUTOR, BATCH, "BATCH_ASSIGNED", ASSIGNED_AT);

        assertEquals(2, notifications.markBatchRead(BATCH, USER, ROLE, READ_AT));
        assertEquals(READ_AT, readAt(assigned));
        assertEquals(READ_AT, readAt(delivered));
        assertNull(readAt(returnNotice));
        assertNull(readAt(otherBatch));
        assertNull(readAt(distributor));
        assertEquals(1, notifications.markBatchRead(BATCH, USER + 1, Role.DISTRIBUTOR, READ_AT));
        assertEquals(READ_AT, readAt(distributor));
    }

    private Notification save(long user, Role role, long batch, String event, Instant createdAt) {
        var notification = new Notification(user, role, batch, event, event, event);
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        return notifications.saveAndFlush(notification);
    }

    private Instant readAt(Notification notification) {
        return notifications.findById(notification.getId()).orElseThrow().getReadAt();
    }
}
