package com.fieldengineering.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.*;
import java.nio.file.*;

@ApplicationScoped
public class LocalStorageService implements StorageService {

    @ConfigProperty(name = "storage.local.directory", defaultValue = "./uploads/drawings")
    String storageDirectory;

    private Path getStoragePath() {
        Path path = Paths.get(storageDirectory).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize storage directory: " + path, e);
            }
        }
        return path;
    }

    @Override
    public String storeFile(String key, InputStream data, String contentType, long size) {
        try {
            Path targetPath = getStoragePath().resolve(key);
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.copy(data, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file with key: " + key, e);
        }
    }

    @Override
    public InputStream getFile(String key) {
        try {
            Path targetPath = getStoragePath().resolve(key);
            if (!Files.exists(targetPath)) {
                throw new RuntimeException("File not found: " + key);
            }
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file with key: " + key, e);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            Path targetPath = getStoragePath().resolve(key);
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file with key: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        Path targetPath = getStoragePath().resolve(key);
        return Files.exists(targetPath);
    }
}
