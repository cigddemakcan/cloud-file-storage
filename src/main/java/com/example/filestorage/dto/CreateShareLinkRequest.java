package com.example.filestorage.dto;

import com.example.filestorage.entity.ShareLink;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateShareLinkRequest(

        @NotNull(message = "fileId bos olamaz")
        @Positive(message = "fileId pozitif bir sayi olmali")
        Long fileId,

        ShareLink.Permission permission,

        @Positive(message = "expiresInHours pozitif olmali")
        @Max(value = 8760, message = "expiresInHours en fazla 8760 (1 yil) olabilir")
        Integer expiresInHours
) {
}
