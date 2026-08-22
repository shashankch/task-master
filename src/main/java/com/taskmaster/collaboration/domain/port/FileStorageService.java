package com.taskmaster.collaboration.domain.port;

public interface FileStorageService {

    String uploadFile(String storageKey, byte[] bytes, String contentType);

    String generatePresignedDownloadUrl(String storageKey, String fileName);

    void deleteFile(String storageKey);
}
