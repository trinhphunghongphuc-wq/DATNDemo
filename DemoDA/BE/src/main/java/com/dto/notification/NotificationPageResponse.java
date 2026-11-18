package com.dto.notification;

import java.util.List;
public record NotificationPageResponse(List<NotificationResponse> items,
        long unreadCount, int page, boolean hasNext) {}
