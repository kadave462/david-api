package com.example.david_api.auth.dto;

public class LoginResponse {
    private final String token;
    private final long expiresInMs;

    public LoginResponse(String token, long expiresInMs) {
        this.token = token;
        this.expiresInMs = expiresInMs;
    }

    public String getToken() {
        return token;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }
}
