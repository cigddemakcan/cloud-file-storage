package com.example.filestorage.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh token gecersiz, suresi dolmus veya iptal edilmis");
    }
}
