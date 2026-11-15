package com.yarden.imageservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        String region,
        String accessKeyId,
        String secretAccessKey,
        String bucket,
        String endpoint,
        String publicEndpoint,
        int presignedUrlMinutes
) {

    /** Empty endpoint = real AWS. */
    public boolean hasEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    /** Host used inside presigned URLs: publicEndpoint if set, otherwise endpoint. */
    public String presignEndpoint() {
        if (publicEndpoint != null && !publicEndpoint.isBlank()) {
            return publicEndpoint;
        }
        return endpoint;
    }
}