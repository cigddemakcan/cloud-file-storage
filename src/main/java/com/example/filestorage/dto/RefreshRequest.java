package com.example.filestorage.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken bos olamaz")
        String refreshToken
) {
}
