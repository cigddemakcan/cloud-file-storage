package com.example.filestorage.dto;

import com.example.filestorage.entity.FileMetadata;

import java.time.Instant;

public record FileMetadataResponse(
        Long id,
        String originalFileName,
        String contentType,
        long size,
        Long parentFolderId,
        Instant createdAt
) {
    public static FileMetadataResponse from(FileMetadata file) {
        return new FileMetadataResponse(
                file.getId(),
                file.getOriginalFileName(),
                file.getContentType(),
                file.getSize(),
                file.getParentFolder() != null ? file.getParentFolder().getId() : null,
                file.getCreatedAt()
        );
    }
}
