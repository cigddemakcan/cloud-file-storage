package com.example.filestorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateFolderRequest(

        @NotBlank(message = "Klasor adi bos olamaz")
        @Size(max = 100, message = "Klasor adi en fazla 100 karakter olabilir")
        String name,

        @Positive(message = "parentFolderId pozitif bir sayi olmali")
        Long parentFolderId
) {
}
