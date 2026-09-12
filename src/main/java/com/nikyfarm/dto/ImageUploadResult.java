package com.nikyfarm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ImageUploadResult {
    private String fileName;
    private boolean success;
    private String imageUrl;
    private String publicId; // Cloudinary's id for this file - needed later to delete/replace it. null if
    // success == false
    private String errorMessage;
}
