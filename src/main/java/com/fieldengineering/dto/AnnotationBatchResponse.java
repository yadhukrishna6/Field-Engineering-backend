package com.fieldengineering.dto;

import com.fieldengineering.domain.MarkupEntity;
import java.util.ArrayList;
import java.util.List;

public class AnnotationBatchResponse {
    public List<MarkupEntity> synced = new ArrayList<>();
    public List<MarkupEntity> conflicts = new ArrayList<>();
    public boolean success = true;
    public String message;
}
