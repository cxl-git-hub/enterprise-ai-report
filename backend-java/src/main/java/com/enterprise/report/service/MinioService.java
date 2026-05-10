package com.enterprise.report.service;

import java.io.InputStream;

public interface MinioService {
    String uploadFile(String objectName, InputStream inputStream, String contentType);
    InputStream downloadFile(String objectName);
    void deleteFile(String objectName);
    String getPresignedUrl(String objectName);
}
