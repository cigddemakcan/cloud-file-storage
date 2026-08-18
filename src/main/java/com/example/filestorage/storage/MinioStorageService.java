package com.example.filestorage.storage;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioStorageService implements StorageService {

    private static final String RESILIENCE_INSTANCE = "minio";

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageService(MinioClient minioClient,
                                @Value("${storage.minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' olusturuldu", bucketName);
            }
        } catch (Exception e) {
            throw new StorageException("MinIO bucket kontrolu/olusturmasi basarisiz", e);
        }
    }

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "uploadFallback")
    public void upload(InputStream inputStream, long contentLength, String contentType, String objectKey) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, contentLength, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Dosya MinIO'ya yuklenemedi: " + objectKey, e);
        }
    }

    private void uploadFallback(InputStream inputStream, long contentLength, String contentType,
                                 String objectKey, Throwable t) {
        throw translateFallbackFailure("yuklenemedi", objectKey, t);
    }

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "downloadFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Dosya MinIO'dan okunamadi: " + objectKey, e);
        }
    }

    private InputStream downloadFallback(String objectKey, Throwable t) {
        throw translateFallbackFailure("okunamadi", objectKey, t);
    }

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "deleteFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build());
        } catch (Exception e) {

            log.error("MinIO nesnesi silinemedi: {}", objectKey, e);
            throw new StorageException("Dosya MinIO'dan silinemedi: " + objectKey, e);
        }
    }

    private void deleteFallback(String objectKey, Throwable t) {
        throw translateFallbackFailure("silinemedi", objectKey, t);
    }

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "presignedUrlFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public String generatePresignedDownloadUrl(String objectKey, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            throw new StorageException("Presigned URL uretilemedi: " + objectKey, e);
        }
    }

    private String presignedUrlFallback(String objectKey, int expirySeconds, Throwable t) {
        throw translateFallbackFailure("presigned URL uretilemedi", objectKey, t);
    }


    private StorageException translateFallbackFailure(String action, String objectKey, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.warn("MinIO circuit breaker ACIK - istek MinIO'ya hic gonderilmedi: {}", objectKey);
            return new StorageException(
                    "Depolama servisi gecici olarak kullanilamiyor, lutfen birazdan tekrar deneyin", t);
        }
        if (t instanceof StorageException storageException) {
            return storageException;
        }
        return new StorageException("Dosya " + action + ": " + objectKey, t);
    }
}
