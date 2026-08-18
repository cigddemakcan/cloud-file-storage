package com.example.filestorage.exception;

public class FolderNotFoundException extends RuntimeException {
    public FolderNotFoundException(Long folderId) {
        super("Klasor bulunamadi veya bu kullaniciya ait degil: " + folderId);
    }
}
