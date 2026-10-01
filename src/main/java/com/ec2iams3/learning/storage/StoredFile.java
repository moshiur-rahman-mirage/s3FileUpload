package com.ec2iams3.learning.storage;

import org.springframework.web.multipart.MultipartFile;

public record StoredFile(String key, String originalFilename, String contentType, long size) {

    public static StoredFile fromUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String originalFilename = StorageKeys.baseName(file.getOriginalFilename());
        return new StoredFile(
                StorageKeys.newKey(originalFilename),
                originalFilename,
                file.getContentType(),
                file.getSize());
    }
}