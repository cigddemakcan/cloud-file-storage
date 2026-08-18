package com.example.filestorage.dto;

import com.example.filestorage.entity.Folder;

import java.time.Instant;

public record FolderResponse(
        Long id,
        String name,
        Long parentFolderId,
        Instant createdAt
) {
    public static FolderResponse from(Folder folder) {
        return new FolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getParentFolder() != null ? folder.getParentFolder().getId() : null,
                folder.getCreatedAt()
        );
    }
}
