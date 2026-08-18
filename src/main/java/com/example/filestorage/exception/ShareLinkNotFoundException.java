package com.example.filestorage.exception;

public class ShareLinkNotFoundException extends RuntimeException {
    public ShareLinkNotFoundException() {
        super("Paylasim linki bulunamadi");
    }
}
