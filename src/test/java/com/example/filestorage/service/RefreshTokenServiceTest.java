package com.example.filestorage.service;

import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.RefreshToken;
import com.example.filestorage.entity.User;
import com.example.filestorage.exception.InvalidRefreshTokenException;
import com.example.filestorage.exception.RefreshTokenReuseDetectedException;
import com.example.filestorage.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService - unit testler (Mockito)")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AuditLogService auditLogService;

    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository, auditLogService, 604_800_000L);

        user = new User();
        user.setId(1L);
    }

    @Test
    @DisplayName("createFor gelecekte gecerli bir expiresAt ile token uretir ve ham token'i doner")
    void createFor_generatesTokenWithFutureExpiryAndReturnsRawToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.IssuedRefreshToken result = refreshTokenService.createFor(user);

        assertThat(result.rawToken()).isNotBlank();
        assertThat(result.user()).isEqualTo(user);


        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(result.rawToken());
        assertThat(captor.getValue().getTokenHash()).isNotBlank();
        assertThat(captor.getValue().getTokenFamily()).isNotBlank();
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Var olmayan token ile validateAndRotate InvalidRefreshTokenException firlatir")
    void validateAndRotate_whenTokenNotFound_throwsException() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("yok-boyle-bir-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Suresi gecmis (ama revoke edilmemis) token ile InvalidRefreshTokenException firlatir")
    void validateAndRotate_whenExpired_throwsInvalidNotReuse() {
        RefreshToken expired = new RefreshToken();
        expired.setTokenHash("hash-degeri");
        expired.setTokenFamily("family-1");
        expired.setUser(user);
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        expired.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("eski-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);


        verify(refreshTokenRepository, never()).revokeAllInFamily(anyString());
    }

    @Test
    @DisplayName("REUSE DETECTION: zaten iptal edilmis token tekrar sunulursa, tum family iptal edilir ve ozel exception firlar")
    void validateAndRotate_whenAlreadyRevoked_detectsReuseAndRevokesFamily() {
        RefreshToken alreadyUsed = new RefreshToken();
        alreadyUsed.setId(99L);
        alreadyUsed.setTokenHash("hash-degeri");
        alreadyUsed.setTokenFamily("family-abc");
        alreadyUsed.setUser(user);
        alreadyUsed.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        alreadyUsed.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(alreadyUsed));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("calinmis-token"))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);

        verify(refreshTokenRepository, times(1)).revokeAllInFamily("family-abc");


        verify(auditLogService, times(1)).log(
                eq(1L), eq(AuditAction.TOKEN_REUSE_DETECTED), eq(99L), any(), eq(false), anyString());


        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Gecerli token ile validateAndRotate: eskisi iptal edilir, yenisi ayni family'de uretilir")
    void validateAndRotate_whenValid_revokesOldAndCreatesNewInSameFamily() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setTokenHash("eski-hash");
        oldToken.setTokenFamily("family-xyz");
        oldToken.setUser(user);
        oldToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        oldToken.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.IssuedRefreshToken result = refreshTokenService.validateAndRotate("gecerli-token");

        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(result.rawToken()).isNotBlank();
        assertThat(result.user()).isEqualTo(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(1).getTokenFamily()).isEqualTo("family-xyz");
    }

    @Test
    @DisplayName("revoke var olan token'i revoked=true yapar")
    void revoke_whenTokenExists_marksRevoked() {
        RefreshToken token = new RefreshToken();
        token.setTokenHash("cikis-hash");
        token.setUser(user);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        refreshTokenService.revoke("cikis-token");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("revoke var olmayan token icin sessizce hicbir sey yapmaz (hata firlatmaz)")
    void revoke_whenTokenDoesNotExist_doesNothingSilently() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        refreshTokenService.revoke("yok");

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
