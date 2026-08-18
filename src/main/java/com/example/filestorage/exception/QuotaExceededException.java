package com.example.filestorage.exception;

public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(long requested, long remaining) {
        super(String.format(
                "Depolama kotasi yetersiz. Istenen: %d byte, kalan: %d byte",
                requested, remaining));
    }
}
