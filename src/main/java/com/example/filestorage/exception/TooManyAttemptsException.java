package com.example.filestorage.exception;

import java.time.Duration;

public class TooManyAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyAttemptsException(Duration retryAfter) {
        super("Cok fazla basarisiz giris denemesi. Lutfen "
                + retryAfter.toMinutes() + " dakika sonra tekrar deneyin.");
        this.retryAfterSeconds = Math.max(1, retryAfter.getSeconds());
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
