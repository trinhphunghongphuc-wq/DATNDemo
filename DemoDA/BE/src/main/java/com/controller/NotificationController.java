package com.controller;

import com.dto.notification.*;
import com.security.CustomUserDetails;
import com.security.JwtService;
import com.service.impl.NotificationService;
import com.service.impl.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'DISTRIBUTOR', 'RETAILER')")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;
    private final NotificationStreamService streams;
    private final JwtService jwt;

    @GetMapping
    public NotificationPageResponse list(@AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return notifications.list(user, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> count(@AuthenticationPrincipal CustomUserDetails user) {
        return Map.of("unreadCount", notifications.unreadCount(user));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse read(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        return notifications.read(user, id);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal CustomUserDetails user,
            @RequestHeader("Authorization") String authorization) {
        return streams.subscribe(user, jwt.extractExpiration(authorization.substring(7)).toInstant());
    }
}
