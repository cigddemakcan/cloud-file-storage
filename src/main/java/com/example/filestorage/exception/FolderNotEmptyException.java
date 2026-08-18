package com.example.filestorage.exception;

public class FolderNotEmptyException extends RuntimeException {
    public FolderNotEmptyException(Long folderId) {
        super("Klasor bos degil, once icindekileri silin veya tasiyin: " + folderId);
    }
}
