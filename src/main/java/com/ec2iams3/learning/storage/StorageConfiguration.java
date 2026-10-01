package com.ec2iams3.learning.storage;

import com.ec2iams3.learning.storage.local.LocalFileStorage;
import com.ec2iams3.learning.storage.s3.S3FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class StorageConfiguration {

    @Value("${file.storage.provider}")
    private String provider;

    @Value("${file.server.path}")
    private String serverPath;

    @Value("${file.storage.s3.bucket}")
    private String s3Bucket;

    @Value("${file.storage.s3.region}")
    private String s3Region;

    @Value("${file.storage.s3.prefix}")
    private String s3Prefix;

    @Value("${file.storage.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${file.storage.s3.path-style-access:false}")
    private boolean s3PathStyleAccess;

    @Bean
    FileStorage fileStorage() {

        return switch (provider.trim().toLowerCase(Locale.ROOT)) {
            case "disk", "local" ->
                    new LocalFileStorage(Path.of(serverPath));

            case "s3" ->
                    S3FileStorage.create(
                            s3Bucket,
                            s3Region,
                            s3Prefix,
                            s3Endpoint,
                            s3PathStyleAccess
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported file.storage.provider: " + provider
                    );
        };
    }
}