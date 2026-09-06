package com.nikyfarm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BulkImageUploadResponse {
    private int successCount;
    private int failureCount;
    private List<ImageUploadResult> results;
}
