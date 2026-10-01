package com.ec2iams3.learning.storage.s3;


import com.ec2iams3.learning.storage.FileStorage;
import com.ec2iams3.learning.storage.StorageKeys;
import com.ec2iams3.learning.storage.StoredFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements AutoCloseable: Spring infers close() as the bean destroy method,
 * so the jakarta.annotation.PreDestroy dependency is no longer needed.
 */
public class S3FileStorage implements FileStorage, AutoCloseable {

    private final S3Client s3Client;
    private final String bucket;
    private final String prefix;

    /** Injectable client -> easy to unit test with a mock/stub. */
    public S3FileStorage(S3Client s3Client, String bucket, String prefix) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3_BUCKET is required when file.storage.provider=s3");
        }
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
    }

    public static S3FileStorage create(String bucket, String region, String prefix,
                                       String endpoint, boolean pathStyleAccess) {
        if (bucket == null || bucket.isBlank()) { // fail before building a client
            throw new IllegalArgumentException("S3_BUCKET is required when file.storage.provider=s3");
        }
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build());
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return new S3FileStorage(builder.build(), bucket, prefix);
    }

    @Override
    public StoredFile upload(MultipartFile file, String folder) throws IOException {
        StoredFile storedFile = StoredFile.fromUpload(file, folder);
        String contentType = storedFile.contentType() == null || storedFile.contentType().isBlank()
                ? "application/octet-stream"
                : storedFile.contentType();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey(storedFile.key()))
                .contentType(contentType)
                .contentLength(storedFile.size())
                .build();
        try (InputStream input = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(input, storedFile.size()));
        } catch (SdkException e) {
            throw new IOException("S3 upload failed for " + storedFile.key(), e);
        }
        return storedFile;
    }

    @Override
    public List<String> list() throws IOException {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucket);

            if (!prefix.isEmpty()) {
                requestBuilder.prefix(prefix + "/");
            }

            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response;
            try {
                response = s3Client.listObjectsV2(requestBuilder.build());
            } catch (SdkException e) {
                throw new IOException("S3 list failed", e);
            }

            for (S3Object s3Object : response.contents()) {
                String objectKey = s3Object.key();
                if (prefix.isEmpty()) {
                    keys.add(objectKey);
                } else if (objectKey.startsWith(prefix + "/")) {
                    keys.add(objectKey.substring(prefix.length() + 1));
                }
            }

            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null && !continuationToken.isBlank());

        return keys;
    }

    @Override
    public Resource download(String key) throws IOException {
        StorageKeys.validate(key);
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(objectKey(key)).build());
            return new S3Resource(response, key);
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException(key);
        } catch (SdkException e) {
            throw new IOException("S3 download failed for " + key, e);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        StorageKeys.validate(key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(objectKey(key)).build());
        } catch (SdkException e) {
            throw new IOException("S3 delete failed for " + key, e);
        }
    }

    @Override
    public void close() {
        s3Client.close();
    }

    private String objectKey(String key) {
        return prefix.isEmpty() ? key : prefix + "/" + key;
    }

    private static String normalizePrefix(String prefix) {
        String normalized = prefix == null ? "" : prefix.trim().replace('\\', '/');
        int start = 0;
        int end = normalized.length();
        while (start < end && normalized.charAt(start) == '/') start++;
        while (end > start && normalized.charAt(end - 1) == '/') end--;
        return normalized.substring(start, end);
    }

    /**
     * A plain InputStreamResource has no length, and Spring's contentLength()
     * on it would read (and consume) the entire stream. This wrapper reports the
     * length S3 already returned in the response headers, so Content-Length is
     * set correctly and the body streams straight through.
     */
    private static final class S3Resource extends InputStreamResource {
        private final long contentLength;
        private final String filename;

        S3Resource(ResponseInputStream<GetObjectResponse> stream, String filename) {
            super(stream, "S3 object [" + filename + "]");
            this.contentLength = stream.response().contentLength();
            this.filename = filename;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}