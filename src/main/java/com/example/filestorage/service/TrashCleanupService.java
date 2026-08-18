package com.example.filestorage.service;

import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.FileMetadata;
import com.example.filestorage.metrics.FileStorageMetrics;
import com.example.filestorage.repository.FileMetadataRepository;
import com.example.filestorage.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrashCleanupService {

    private final FileMetadataRepository fileMetadataRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final FileStorageMetrics metrics;

    @Value("${trash.retention-days:30}")
    private int retentionDays;


    @Scheduled(cron = "${trash.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredTrash() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        List<FileMetadata> expired =
                fileMetadataRepository.findAllByDeletedTrueAndDeletedAtBefore(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Trash temizligi basliyor: {} dosya kalici silinecek", expired.size());

        int successCount = 0;
        int failureCount = 0;

        for (FileMetadata file : expired) {
            try {
                storageService.delete(file.getStorageKey());
                Long ownerId = file.getOwner().getId();
                Long fileId = file.getId();

                fileMetadataRepository.delete(file);


                auditLogService.log(ownerId, AuditAction.PURGE, fileId, null, true,
                        "trash retention (" + retentionDays + " gun) doldu");

                metrics.incrementPurge();
                successCount++;
            } catch (Exception e) {

                log.error("Trash temizligi sirasinda dosya silinemedi: id={}, key={}",
                        file.getId(), file.getStorageKey(), e);
                failureCount++;
            }
        }

        log.info("Trash temizligi bitti: {} basarili, {} basarisiz", successCount, failureCount);
    }
}
