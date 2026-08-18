package com.example.filestorage.security;

import com.example.filestorage.exception.TooManyAttemptsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoginAttemptService - unit testler")
class LoginAttemptServiceTest {

    @Test
    @DisplayName("Hic basarisiz deneme yokken assertNotBlocked hicbir sey firlatmaz")
    void assertNotBlocked_withNoAttempts_doesNotThrow() {
        LoginAttemptService service = new LoginAttemptService();

        assertThatCode(() -> service.assertNotBlocked("yeni_kullanici"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("5'ten az basarisiz denemede kilit devreye girmez")
    void fourFailures_doesNotLockAccount() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 4; i++) {
            service.recordFailure("cigdem");
        }

        assertThatCode(() -> service.assertNotBlocked("cigdem"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("5. basarisiz denemeden sonra hesap kilitlenir ve TooManyAttemptsException firlar")
    void fifthFailure_locksAccount() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 5; i++) {
            service.recordFailure("cigdem");
        }

        assertThatThrownBy(() -> service.assertNotBlocked("cigdem"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    @DisplayName("recordSuccess basarisiz deneme sayacini sifirlar")
    void recordSuccess_resetsFailureCount() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 4; i++) {
            service.recordFailure("cigdem");
        }
        service.recordSuccess("cigdem");


        for (int i = 0; i < 4; i++) {
            service.recordFailure("cigdem");
        }

        assertThatCode(() -> service.assertNotBlocked("cigdem"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Farkli kullanicilarin basarisiz deneme sayaclari birbirini etkilemez")
    void differentUsers_haveIndependentCounters() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 5; i++) {
            service.recordFailure("saldirgan_hedefi");
        }

        assertThatCode(() -> service.assertNotBlocked("masum_kullanici"))
                .doesNotThrowAnyException();
    }
}
