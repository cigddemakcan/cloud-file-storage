package com.example.filestorage.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Kullanici adi bos olamaz")
        String username,

        @NotBlank(message = "Sifre bos olamaz")
        String password
) {
}
