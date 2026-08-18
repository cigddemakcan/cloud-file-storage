package com.example.filestorage.exception;

public class ShareLinkExpiredException extends RuntimeException {
    public ShareLinkExpiredException() {
        super("Paylasim linkinin suresi dolmus veya iptal edilmis");
    }
}
