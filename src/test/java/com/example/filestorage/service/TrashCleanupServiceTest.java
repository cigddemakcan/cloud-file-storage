package com.example.filestorage.service;

import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.entity.User;
import com.example.filestorage.metrics.FileStorageMetrics;
import com.example.filestorage.repository.FileMetadataRepository;
import com.example.filestorage.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("TrashCleanupService (trash purge job) - unit testler (Mockito)")
class TrashCleanupServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private FileStorageMetrics metrics;

    private TrashCleanupService trashCleanupService;

    @BeforeEach
    void setUp() {
        trashCleanupService = new TrashCleanupService(
                fileMetadataRepository, storageService, auditLogService, metrics);


        ReflectionTestUtils.setField(trashCleanupService, "retentionDays", 30);
    }

    private FileMetadata buildExpiredFile(Long id, Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);

        FileMetadata file = new FileMetadata();
        file.setId(id);
        file.setOwner(owner);
        file.setStorageKey("users/" + ownerId + "/" + id);
        return file;
    }

    @Test
    @DisplayName("Silinecek dosya yoksa hicbir yere dokunulmaz")
    void purgeExpiredTrash_whenNoExpiredFiles_doesNothing() {
        when(fileMetadataRepository.findAllByDeletedTrueAndDeletedAtBefore(any())).thenReturn(List.of());

        trashCleanupService.purgeExpiredTrash();

        verify(storageService, never()).delete(anyString());
        verify(fileMetadataRepository, never()).delete(any());
        verify(metrics, never()).incrementPurge();
    }

    @Test
    @DisplayName("Suresi dolmus dosyalar MinIO'dan ve DB'den kalici silinir, audit loglanir, metrik artar")
    void purgeExpiredTrash_whenExpiredFilesExist_purgesEachOne() {
        FileMetadata file1 = buildExpiredFile(1L, 100L);
        FileMetadata file2 = buildExpiredFile(2L, 200L);

        when(fileMetadataRepository.findAllByDeletedTrueAndDeletedAtBefore(any()))
                .thenReturn(List.of(file1, file2));

        trashCleanupService.purgeExpiredTrash();

        verify(storageService, times(1)).delete(file1.getStorageKey());
        verify(storageService, times(1)).delete(file2.getStorageKey());
        verify(fileMetadataRepository, times(1)).delete(file1);
        verify(fileMetadataRepository, times(1)).delete(file2);
        verify(metrics, times(2)).incrementPurge();

        verify(auditLogService, times(1)).log(
                eq(100L), eq(AuditAction.PURGE), eq(1L), isNull(), eq(true), anyString());
        verify(auditLogService, times(1)).log(
                eq(200L), eq(AuditAction.PURGE), eq(2L), isNull(), eq(true), anyString());
    }

    @Test
    @DisplayName("Bir dosyanin silinmesi basarisiz olursa diger dosyalar yine de islenir (kendini toparlayan davranis)")
    void purgeExpiredTrash_whenOneFileFails_continuesWithOthers() {
        FileMetadata failingFile = buildExpiredFile(1L, 100L);
        FileMetadata healthyFile = buildExpiredFile(2L, 200L);

        when(fileMetadataRepository.findAllByDeletedTrueAndDeletedAtBefore(any()))
                .thenReturn(List.of(failingFile, healthyFile));


        doThrow(new RuntimeException("MinIO gecici olarak erisilemez"))
                .when(storageService).delete(failingFile.getStorageKey());

        trashCleanupService.purgeExpiredTrash();

        verify(fileMetadataRepository, never()).delete(failingFile);


        verify(storageService, times(1)).delete(healthyFile.getStorageKey());
        verify(fileMetadataRepository, times(1)).delete(healthyFile);


        verify(metrics, times(1)).incrementPurge();
    }
}
