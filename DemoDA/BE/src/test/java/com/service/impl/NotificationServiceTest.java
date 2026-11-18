package com.service.impl;

import com.entity.Batch;
import com.entity.Notification;
import com.entity.User;
import com.enums.Role;
import com.repository.NotificationRepository;
import com.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {
    private NotificationService service;
    private CustomUserDetails user;
    private List<Notification> saved;
    private List<Object> events;
    private Optional<Notification> owned;
    private int markCalls;
    private int markResult;
    private Long queriedRecipient;
    private Role queriedRole;

    @BeforeEach
    void setup() {
        saved = new ArrayList<>();
        events = new ArrayList<>();
        owned = Optional.empty();
        markCalls = 0;
        markResult = 0;
        // An interface stub avoids instrumentation/JVM agent requirements.
        NotificationRepository repository = (NotificationRepository) Proxy.newProxyInstance(
                NotificationRepository.class.getClassLoader(),
                new Class<?>[]{NotificationRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "save":
                            saved.add((Notification) args[0]);
                            return args[0];
                        case "findByIdAndRecipientIdAndRecipientRole":
                            queriedRecipient = (Long) args[1];
                            queriedRole = (Role) args[2];
                            return owned;
                        case "markRead":
                            markCalls++;
                            assertEquals(user.getId(), args[1]);
                            assertEquals(user.getRole(), args[2]);
                            return markResult;
                        case "findByRecipientIdAndRecipientRole":
                            queriedRecipient = (Long) args[0];
                            queriedRole = (Role) args[1];
                            return new PageImpl<Notification>(List.of());
                        case "countByRecipientIdAndRecipientRoleAndReadAtIsNull":
                            assertEquals(user.getId(), args[0]);
                            assertEquals(user.getRole(), args[1]);
                            return 3L;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
        service = new NotificationService(repository, events::add);
        User account = new User();
        account.setId(10L);
        account.setUsername("distributor-a");
        account.setRole(Role.DISTRIBUTOR);
        account.setEnabled(true);
        user = new CustomUserDetails(account);
    }

    private Batch batch() {
        return Batch.builder().id(5L).batchCode("BATCH-5")
                .distributorId(10L).retailerId(20L).build();
    }

    @Test
    void assignmentTargetsOnlyAssignedDistributorAndRetailer() {
        service.batchAssigned(batch());
        assertEquals(2, saved.size());
        assertEquals(10L, saved.get(0).getRecipientId());
        assertEquals(Role.DISTRIBUTOR, saved.get(0).getRecipientRole());
        assertEquals(20L, saved.get(1).getRecipientId());
        assertEquals(Role.RETAILER, saved.get(1).getRecipientRole());
        assertEquals("BATCH_ASSIGNED", saved.get(1).getEventType());
        assertTrue(saved.get(1).getMessage().contains("Đang chờ"));
        assertEquals("/retailer/batches/5/records",
                com.dto.notification.NotificationResponse.from(saved.get(1)).targetUrl());
        assertEquals(List.of(new NotificationChanged(10L), new NotificationChanged(20L)), events);
    }

    @Test
    void deliveryTargetsOnlyAssignedRetailer() {
        service.batchDelivered(batch());
        assertEquals(1, saved.size());
        assertEquals(20L, saved.get(0).getRecipientId());
        assertEquals(Role.RETAILER, saved.get(0).getRecipientRole());
        assertEquals(List.of(new NotificationChanged(20L)), events);
    }

    @Test
    void noAssignmentCreatesNoNotification() {
        service.batchAssigned(Batch.builder().id(5L).build());
        assertTrue(saved.isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    void cannotMarkSomeoneElsesNotificationRead() {
        assertThrows(AccessDeniedException.class, () -> service.read(user, 99L));
        assertEquals(user.getId(), queriedRecipient);
        assertEquals(user.getRole(), queriedRole);
        assertEquals(0, markCalls);
        assertTrue(events.isEmpty());
    }

    @Test
    void alreadyReadDoesNotPublishAnotherChange() {
        owned = Optional.of(new Notification(10L, Role.DISTRIBUTOR, 5L,
                "BATCH_ASSIGNED", "New batch", "Batch 5"));
        service.read(user, 99L);
        assertEquals(1, markCalls);
        assertTrue(events.isEmpty());
    }

    @Test
    void firstReadPublishesChangeAndNavigatesToAssignedBatch() {
        owned = Optional.of(new Notification(10L, Role.DISTRIBUTOR, 5L,
                "BATCH_ASSIGNED", "New batch", "Batch 5"));
        markResult = 1;
        assertEquals("/distributor/batches/5", service.read(user, 99L).targetUrl());
        assertEquals(List.of(new NotificationChanged(10L)), events);
    }

    @Test
    void listDoesNotMarkAnythingReadAndIsScopedToCurrentAccount() {
        assertEquals(3L, service.list(user, 0, 20).unreadCount());
        assertEquals(user.getId(), queriedRecipient);
        assertEquals(user.getRole(), queriedRole);
        assertEquals(0, markCalls);
        assertTrue(events.isEmpty());
    }

    @Test
    void excessivePageSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.list(user, 0, 1000));
        assertNull(queriedRecipient);
    }
}
