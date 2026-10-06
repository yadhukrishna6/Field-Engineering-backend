package com.fieldengineering.service;

import java.io.InputStream;

public interface StorageService {
    String storeFile(String key, InputStream data, String contentType, long size);
    InputStream getFile(String key);
    void deleteFile(String key);
    boolean exists(String key);
}
