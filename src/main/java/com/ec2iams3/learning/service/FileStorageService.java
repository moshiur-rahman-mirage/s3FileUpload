package com.ec2iams3.learning.service;

import com.ec2iams3.learning.storage.FileStorage;
import com.ec2iams3.learning.storage.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class FileStorageService {

    private final FileStorage fileStorage;

    public FileStorageService(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public String upload(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (category == null) {
            throw new IllegalArgumentException("File category is required");
        }

        try {
            StoredFile storedFile = fileStorage.upload(file);
            return storedFile.key();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public List<String> listFiles() {
        try {
            return fileStorage.list();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list files", e);
        }
    }

    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        try {
            fileStorage.delete(fileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + fileName, e);
        }
    }

    public Resource downloadFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        try {
            return fileStorage.download(fileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file: " + fileName, e);
        }
    }

    public enum FileCategory {
        INVOICE,
        PROFILE_IMAGE
    }
}
