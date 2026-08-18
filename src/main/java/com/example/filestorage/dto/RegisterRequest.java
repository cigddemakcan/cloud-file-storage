package com.example.filestorage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Kullanici adi bos olamaz")
        @Size(min = 3, max = 50, message = "Kullanici adi 3-50 karakter arasinda olmali")
        String username,

        @NotBlank(message = "Email bos olamaz")
        @Email(message = "Gecerli bir email adresi girin")
        String email,

        @NotBlank(message = "Sifre bos olamaz")
        @Size(min = 8, max = 100, message = "Sifre en az 8 karakter olmali")
        String password
) {
}
