package com.yarden.imageservice.service;

import com.yarden.imageservice.config.S3Properties;
import com.yarden.imageservice.exception.StorageUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Service
public class S3StorageService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private S3Properties props;

    public void upload(String key, String contentType, InputStream content, long size) {
        try {
            s3Client.putObject(
                    b -> b.bucket(props.bucket()).key(key).contentType(contentType),
                    RequestBody.fromInputStream(content, size));
        } catch (SdkException e) {
            throw new StorageUnavailableException(e);
        }
    }

    public void delete(String key) {
        try {
            s3Client.deleteObject(b -> b.bucket(props.bucket()).key(key));
        } catch (SdkException e) {
            throw new StorageUnavailableException(e);
        }
    }

    public boolean exists(String key) {
        try {
            s3Client.headObject(b -> b.bucket(props.bucket()).key(key));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new StorageUnavailableException(e);
        } catch (SdkException e) {
            throw new StorageUnavailableException(e);
        }
    }

    public URI presignedGetUrl(String key) {
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofMinutes(props.presignedUrlMinutes()))
                .getObjectRequest(g -> g.bucket(props.bucket()).key(key)));
        return URI.create(presigned.url().toString());
    }
}