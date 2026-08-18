package com.example.filestorage.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class FileStorageMetrics {

    private final Counter uploadCounter;
    private final Counter uploadFailedCounter;
    private final Counter downloadCounter;
    private final Counter restoreCounter;
    private final Counter purgeCounter;

    public FileStorageMetrics(MeterRegistry registry) {
        this.uploadCounter = Counter.builder("filestorage.files.uploaded")
                .description("Basariyla yuklenen toplam dosya sayisi")
                .register(registry);

        this.uploadFailedCounter = Counter.builder("filestorage.files.upload_failed")
                .description("Yukleme sirasinda basarisiz olan (rollback edilen) islemler")
                .register(registry);

        this.downloadCounter = Counter.builder("filestorage.files.downloaded")
                .description("Toplam indirme islemi sayisi")
                .register(registry);

        this.restoreCounter = Counter.builder("filestorage.files.restored")
                .description("Trash'ten geri yuklenen toplam dosya sayisi")
                .register(registry);

        this.purgeCounter = Counter.builder("filestorage.files.purged")
                .description("TrashCleanupService tarafindan kalici silinen toplam dosya sayisi")
                .register(registry);
    }

    public void incrementUpload() {
        uploadCounter.increment();
    }

    public void incrementUploadFailed() {
        uploadFailedCounter.increment();
    }

    public void incrementDownload() {
        downloadCounter.increment();
    }

    public void incrementRestore() {
        restoreCounter.increment();
    }

    public void incrementPurge() {
        purgeCounter.increment();
    }
}
