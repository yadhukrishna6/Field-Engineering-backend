package com.fieldengineering.dto;

import java.time.Instant;

public class DrawingResponseDto {
    public String id;
    public String name;
    public String fileType;
    public int pageCount;
    public long fileSize;
    public String ownerId;
    public Instant createdAt;
    public String fileUrl;
}
