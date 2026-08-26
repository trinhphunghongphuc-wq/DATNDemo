package com.dto.user.admin;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class AdminUserResponse {

    private Long id;
    private String username;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;

    public AdminUserResponse() {
    }

    public AdminUserResponse(Long id, String username, String role, Boolean enabled, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }


}