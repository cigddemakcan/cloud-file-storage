package com.example.filestorage.exception;


public class RefreshTokenReuseDetectedException extends RuntimeException {
    public RefreshTokenReuseDetectedException() {
        super("Refresh token yeniden kullanim tespit edildi - guvenlik nedeniyle "
                + "bu oturum zinciri tamamen iptal edildi, lutfen tekrar giris yapin");
    }
}
