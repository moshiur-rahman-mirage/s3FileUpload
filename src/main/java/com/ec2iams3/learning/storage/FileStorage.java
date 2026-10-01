package com.ec2iams3.learning.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface FileStorage {

    /** Stores the upload under a generated key. */
    default StoredFile upload(MultipartFile file) throws IOException {
        return upload(file, "");
    }

    /** Stores the upload under a generated key inside a folder/category path. */
    StoredFile upload(MultipartFile file, String folder) throws IOException;

    /** Lists all stored keys in the backend. */
    List<String> list() throws IOException;

    /**
     * @throws FileNotFoundException if no file exists for the key
     * @throws IllegalArgumentException if the key is malformed
     */
    Resource download(String key) throws IOException;

    /** Idempotent: deleting a missing key is not an error. */
    void delete(String key) throws IOException;
}