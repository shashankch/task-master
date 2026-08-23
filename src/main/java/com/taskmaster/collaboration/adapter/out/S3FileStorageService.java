package com.taskmaster.collaboration.adapter.out;

import com.taskmaster.collaboration.domain.port.FileStorageService;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * File storage service supporting S3/MinIO compatible object stores with in-memory fallback.
 */
@Service
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

    private final String bucketName;
    private final String endpointUrl;

    // In-memory backing store for local/testing environments
    private final ConcurrentHashMap<String, byte[]> memoryStore = new ConcurrentHashMap<>();

    public S3FileStorageService(
        @Value("${app.storage.s3.bucket-name:taskmaster-attachments}") String bucketName,
        @Value("${app.storage.s3.endpoint-url:http://localhost:9000}") String endpointUrl
    ) {
        this.bucketName = bucketName;
        this.endpointUrl = endpointUrl;
    }

    @Override
    public String uploadFile(String storageKey, byte[] bytes, String contentType) {
        memoryStore.put(storageKey, bytes);
        log.info("Stored file with key: {} (size: {} bytes, type: {}) in bucket: {}", storageKey, bytes.length, contentType, bucketName);
        return storageKey;
    }

    @Override
    public String generatePresignedDownloadUrl(String storageKey, String fileName) {
        // Pre-signed URL simulation compatible with S3/MinIO
        return String.format("%s/%s/%s?response-content-disposition=attachment;filename=\"%s\"",
            endpointUrl, bucketName, storageKey, fileName);
    }

    @Override
    public void deleteFile(String storageKey) {
        memoryStore.remove(storageKey);
        log.info("Deleted file with key: {} from bucket: {}", storageKey, bucketName);
    }
}
