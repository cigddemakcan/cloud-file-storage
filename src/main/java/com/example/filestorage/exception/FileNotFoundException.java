package com.example.filestorage.exception;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(Long fileId) {
        super("Dosya bulunamadi veya bu kullaniciya ait degil: " + fileId);
    }
}
