package com.nikyfarm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ImageUploadResponse {
    private Long publicId;
    private String imageUrl;
    private String message;
}
