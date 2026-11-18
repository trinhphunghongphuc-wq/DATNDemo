package com.service.impl;

import com.enums.Role;
import com.repository.UserRepository;
import com.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationStreamService {
    private final UserRepository users;
    private final Map<UUID, Client> clients = new ConcurrentHashMap<>();
    private record Client(Long userId, Role role, Instant expiresAt, Instant reconnectAt, SseEmitter emitter) {}

    public SseEmitter subscribe(CustomUserDetails user, Instant expiresAt) {
        // Do not use a Servlet async timeout to rotate an otherwise healthy stream.
        // Heartbeats below revalidate the account and complete it cleanly for reconnect.
        SseEmitter emitter = new SseEmitter(0L);
        UUID id = UUID.randomUUID();
        Client client = new Client(user.getId(), user.getRole(), expiresAt,
                Instant.now().plusSeconds(15 * 60), emitter);
        clients.put(id, client);
        emitter.onCompletion(() -> clients.remove(id));
        emitter.onTimeout(() -> clients.remove(id));
        emitter.onError(error -> clients.remove(id));
        send(id, client, "ready");
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void changed(NotificationChanged event) {
        clients.forEach((id, client) -> {
            if (client.userId().equals(event.recipientId()) && allowed(client)) {
                send(id, client, "notifications-changed");
            }
        });
    }

    @Scheduled(fixedDelay = 20_000)
    public void heartbeat() {
        clients.forEach((id, client) -> {
            if (Instant.now().isBefore(client.reconnectAt()) && allowed(client)) send(id, client, "heartbeat");
            else { clients.remove(id); client.emitter().complete(); }
        });
    }

    private boolean allowed(Client client) {
        try {
            return Instant.now().isBefore(client.expiresAt()) && users.findById(client.userId())
                    .filter(user -> Boolean.TRUE.equals(user.getEnabled()) && user.getRole() == client.role())
                    .isPresent();
        } catch (RuntimeException error) {
            // A delivery failure must never turn a committed batch operation into HTTP 500.
            return false;
        }
    }

    private void send(UUID id, Client client, String name) {
        try {
            client.emitter().send(SseEmitter.event().name(name).data("refresh"));
        } catch (IOException error) {
            // Servlet/Spring owns error completion after a failed write. Calling
            // complete() here races that cleanup and causes another async dispatch.
            clients.remove(id);
        } catch (IllegalStateException error) {
            // An already completed/errored emitter needs no second completion.
            clients.remove(id);
        }
    }
}
