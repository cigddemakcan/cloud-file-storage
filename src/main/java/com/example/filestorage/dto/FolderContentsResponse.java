package com.example.filestorage.dto;

import java.util.List;

public record FolderContentsResponse(
        List<FolderResponse> folders,
        List<FileMetadataResponse> files
) {
}
