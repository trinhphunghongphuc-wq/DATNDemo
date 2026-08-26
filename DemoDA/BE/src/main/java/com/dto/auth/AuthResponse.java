package com.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String message;
    private String username;
    private String role;
    private String token;

    public AuthResponse() {
    }

    public AuthResponse( String username, String role, String token) {

        this.username = username;
        this.role = role;
        this.token = token;
    }

    public AuthResponse(String message) {
        this.message = message;
    }



}