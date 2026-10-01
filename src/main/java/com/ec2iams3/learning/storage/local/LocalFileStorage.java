package com.ec2iams3.learning.storage.local;


import com.ec2iams3.learning.storage.FileStorage;
import com.ec2iams3.learning.storage.StorageKeys;
import com.ec2iams3.learning.storage.StoredFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
        try {
            // Once at startup instead of on every upload; also fails fast on bad config.
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root: " + this.root, e);
        }
    }

    @Override
    public StoredFile upload(MultipartFile file, String folder) throws IOException {
        StoredFile storedFile = StoredFile.fromUpload(file, folder);
        Path destination = resolve(storedFile.key());
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try {
            file.transferTo(destination);
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        }
        return storedFile;
    }

    @Override
    public List<String> list() throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .toList();
        }
    }

    @Override
    public Resource download(String key) throws IOException {
        Path path = resolve(key);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new FileNotFoundException(key);
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    private Path resolve(String key) throws IOException {
        StorageKeys.validate(key);
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) { // defence in depth
            throw new IOException("Invalid file key");
        }
        return path;
    }
}