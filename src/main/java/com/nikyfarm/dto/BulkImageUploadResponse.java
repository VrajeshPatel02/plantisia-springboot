package com.nikyfarm.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BulkImageUploadResponse {
    private int successCount;
    private int failureCount;
    private List<ImageUploadResult> results;
}
