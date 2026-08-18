package com.example.filestorage.health;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component("minio")
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioHealthIndicator(
            MinioClient minioClient,
            @Value("${storage.minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public Health health() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());

            if (exists) {
                return Health.up()
                        .withDetail("bucket", bucketName)
                        .build();
            }

            return Health.down()
                    .withDetail("bucket", bucketName)
                    .withDetail("reason", "Bucket bulunamadi")
                    .build();

        } catch (Exception e) {
            log.warn("MinIO health check basarisiz", e);
            return Health.down(e)
                    .withDetail("bucket", bucketName)
                    .build();
        }
    }
}
