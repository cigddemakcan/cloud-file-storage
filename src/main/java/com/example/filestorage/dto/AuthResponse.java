package com.example.filestorage.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {


    public String token() {
        return accessToken();
    }
}
