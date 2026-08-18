package com.example.filestorage.service;

import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.entity.ShareLink;
import com.example.filestorage.entity.User;
import com.example.filestorage.exception.FileNotFoundException;
import com.example.filestorage.exception.ShareLinkExpiredException;
import com.example.filestorage.exception.ShareLinkNotFoundException;
import com.example.filestorage.repository.ShareLinkRepository;
import com.example.filestorage.repository.UserRepository;
import com.example.filestorage.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShareLinkService - unit testler (Mockito)")
class ShareLinkServiceTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileService fileService;
    @Mock
    private StorageService storageService;
    @Mock
    private AuditLogService auditLogService;

    private ShareLinkService shareLinkService;

    private User creator;
    private FileMetadata file;

    @BeforeEach
    void setUp() {
        shareLinkService = new ShareLinkService(
                shareLinkRepository, userRepository, fileService, storageService, auditLogService);

        creator = new User();
        creator.setId(1L);

        file = new FileMetadata();
        file.setId(7L);
        file.setStorageKey("users/1/abc");
    }

    @Test
    @DisplayName("Baskasinin dosyasi icin link olusturmaya calisinca FileService uzerinden reddedilir")
    void createShareLink_forFileNotOwned_propagatesFileNotFoundException() {
        when(fileService.getOwnedFile(7L, 1L)).thenThrow(new FileNotFoundException(7L));

        assertThatThrownBy(() ->
                shareLinkService.createShareLink(1L, 7L, ShareLink.Permission.DOWNLOAD, null))
                .isInstanceOf(FileNotFoundException.class);

        verify(shareLinkRepository, never()).save(any(ShareLink.class));
    }

    @Test
    @DisplayName("Permission ve expiry belirtilmezse varsayilan DOWNLOAD ve 24 saat kullanilir")
    void createShareLink_withoutExplicitPermissionAndExpiry_usesDefaults() {
        when(fileService.getOwnedFile(7L, 1L)).thenReturn(file);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        ShareLink result = shareLinkService.createShareLink(1L, 7L, null, null);
        Instant after = Instant.now();

        assertThat(result.getPermission()).isEqualTo(ShareLink.Permission.DOWNLOAD);
        assertThat(result.getExpiresAt()).isAfter(before.plus(23, ChronoUnit.HOURS));
        assertThat(result.getExpiresAt()).isBefore(after.plus(25, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Var olmayan token ile resolveToken ShareLinkNotFoundException firlatir")
    void resolveToken_whenTokenNotFound_throwsShareLinkNotFoundException() {
        when(shareLinkRepository.findByToken("gecersiz-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareLinkService.resolveToken("gecersiz-token"))
                .isInstanceOf(ShareLinkNotFoundException.class);
    }

    @Test
    @DisplayName("Suresi gecmis link icin ShareLinkExpiredException firlatir")
    void resolveToken_whenExpired_throwsShareLinkExpiredException() {
        ShareLink expiredLink = new ShareLink();
        expiredLink.setToken("eski-token");
        expiredLink.setFile(file);
        expiredLink.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS)); // gecmiste

        when(shareLinkRepository.findByToken("eski-token")).thenReturn(Optional.of(expiredLink));

        assertThatThrownBy(() -> shareLinkService.resolveToken("eski-token"))
                .isInstanceOf(ShareLinkExpiredException.class);
    }

    @Test
    @DisplayName("Iptal edilmis (revoked) link icin ShareLinkExpiredException firlatir")
    void resolveToken_whenRevoked_throwsShareLinkExpiredException() {
        ShareLink revokedLink = new ShareLink();
        revokedLink.setToken("iptal-token");
        revokedLink.setFile(file);
        revokedLink.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        revokedLink.setRevoked(true);

        when(shareLinkRepository.findByToken("iptal-token")).thenReturn(Optional.of(revokedLink));

        assertThatThrownBy(() -> shareLinkService.resolveToken("iptal-token"))
                .isInstanceOf(ShareLinkExpiredException.class);
    }

    @Test
    @DisplayName("Arkasindaki dosya trash'e dusmusse link gecerli olsa bile ShareLinkExpiredException firlatir")
    void resolveToken_whenFileDeleted_throwsShareLinkExpiredException() {
        file.setDeleted(true); // dosya trash'te

        ShareLink link = new ShareLink();
        link.setToken("gecerli-ama-dosya-silinmis");
        link.setFile(file);
        link.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        when(shareLinkRepository.findByToken("gecerli-ama-dosya-silinmis")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> shareLinkService.resolveToken("gecerli-ama-dosya-silinmis"))
                .isInstanceOf(ShareLinkExpiredException.class);
    }

    @Test
    @DisplayName("VIEW izinli link ile indirme denenirse ShareLinkExpiredException firlatir")
    void downloadViaToken_withViewOnlyPermission_throwsException() {
        ShareLink viewOnlyLink = new ShareLink();
        viewOnlyLink.setToken("view-token");
        viewOnlyLink.setFile(file);
        viewOnlyLink.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        viewOnlyLink.setPermission(ShareLink.Permission.VIEW);

        when(shareLinkRepository.findByToken("view-token")).thenReturn(Optional.of(viewOnlyLink));

        assertThatThrownBy(() -> shareLinkService.downloadViaToken("view-token"))
                .isInstanceOf(ShareLinkExpiredException.class);

        verify(storageService, never()).download(anyString());
    }

    @Test
    @DisplayName("DOWNLOAD izinli gecerli link ile indirme basarili olur ve audit log manuel cagrilir")
    void downloadViaToken_withDownloadPermission_succeedsAndLogsManually() {
        ShareLink link = new ShareLink();
        link.setToken("download-token");
        link.setFile(file);
        link.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        link.setPermission(ShareLink.Permission.DOWNLOAD);

        when(shareLinkRepository.findByToken("download-token")).thenReturn(Optional.of(link));

        shareLinkService.downloadViaToken("download-token");

        verify(storageService, times(1)).download(file.getStorageKey());

        verify(auditLogService, times(1)).log(
                isNull(), eq(AuditAction.SHARE_DOWNLOAD), eq(7L), isNull(), eq(true), anyString());
    }

    @Test
    @DisplayName("Baskasina ait/var olmayan share link iptal edilmeye calisilinca ShareLinkNotFoundException firlar")
    void revoke_whenNotFoundForUser_throwsException() {
        when(shareLinkRepository.findByIdAndCreatedById(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareLinkService.revoke(3L, 1L))
                .isInstanceOf(ShareLinkNotFoundException.class);
    }

    @Test
    @DisplayName("Sahibi tarafindan iptal edilince link revoked=true olur")
    void revoke_whenOwnedByUser_marksRevoked() {
        ShareLink link = new ShareLink();
        link.setId(3L);
        link.setCreatedBy(creator);

        when(shareLinkRepository.findByIdAndCreatedById(3L, 1L)).thenReturn(Optional.of(link));

        shareLinkService.revoke(3L, 1L);

        assertThat(link.isRevoked()).isTrue();
        verify(shareLinkRepository, times(1)).save(link);
    }
}
