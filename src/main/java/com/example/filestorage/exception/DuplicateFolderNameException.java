package com.example.filestorage.exception;

public class DuplicateFolderNameException extends RuntimeException {
    public DuplicateFolderNameException(String name) {
        super("Bu isimde bir klasor zaten mevcut: " + name);
    }
}
