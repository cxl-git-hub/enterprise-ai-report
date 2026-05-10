package com.enterprise.report.service.impl;

import com.enterprise.report.config.MinioConfig;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.service.MinioService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String uploadFile(String objectName, InputStream inputStream, String contentType) {
        try {
            ensureBucketExists();
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, inputStream.available(), -1)
                    .contentType(contentType)
                    .build();
            minioClient.putObject(args);
            return objectName;
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String objectName) {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build();
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to download file: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build();
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String objectName) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .expiry(24, TimeUnit.HOURS)
                    .build();
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to generate presigned URL: " + e.getMessage());
        }
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build());
            }
        } catch (Exception e) {
            log.error("Failed to check/create bucket: {}", e.getMessage());
        }
    }
}
