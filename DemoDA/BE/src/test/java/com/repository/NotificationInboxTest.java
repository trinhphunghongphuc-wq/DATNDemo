package com.repository;

import com.entity.Notification;
import com.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:notification-inbox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class}, mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class NotificationInboxTest {
    @Autowired
    private NotificationRepository notifications;

    @Test
    void distributorInboxSkipsLegacyWaitingNoticeBeforePagingAndCounting() {
        var approval = save(1L, Role.DISTRIBUTOR, "RETURN_APPROVED");
        save(1L, Role.DISTRIBUTOR, "RETURN_REQUESTED");
        save(1L, Role.DISTRIBUTOR, "BATCH_ASSIGNED");
        save(2L, Role.PRODUCER, "RETURN_REQUESTED");

        var page = notifications.findByRecipientIdAndRecipientRole(
                1L, Role.DISTRIBUTOR, PageRequest.of(0, 1, Sort.by("id")));
        assertEquals(approval.getId(), page.getContent().get(0).getId());
        assertEquals(2, page.getTotalElements());
        assertEquals(2, notifications.countByRecipientIdAndRecipientRoleAndReadAtIsNull(1L, Role.DISTRIBUTOR));

        notifications.markRead(approval.getId(), 1L, Role.DISTRIBUTOR, Instant.now());
        assertEquals(1, notifications.countByRecipientIdAndRecipientRoleAndReadAtIsNull(1L, Role.DISTRIBUTOR));
        assertEquals(1, notifications.countByRecipientIdAndRecipientRoleAndReadAtIsNull(2L, Role.PRODUCER));
        assertEquals("RETURN_REQUESTED", notifications.findByRecipientIdAndRecipientRole(
                2L, Role.PRODUCER, PageRequest.of(0, 1)).getContent().get(0).getEventType());
    }

    private Notification save(Long userId, Role role, String event) {
        return notifications.saveAndFlush(new Notification(userId, role, 100L, event, "Title", "Message"));
    }
}
