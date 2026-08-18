package com.example.filestorage.storage;

import java.io.InputStream;


public interface StorageService {


    void upload(InputStream inputStream, long contentLength, String contentType, String objectKey);

    InputStream download(String objectKey);

    void delete(String objectKey);

    String generatePresignedDownloadUrl(String objectKey, int expirySeconds);
}
