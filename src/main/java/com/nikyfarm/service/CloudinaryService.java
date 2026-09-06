package com.nikyfarm.service;

import com.nikyfarm.dto.ImageUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloudinaryService {
    ImageUploadResult uploadImage(MultipartFile file, String folder);
    void deleteImage(String publicId);
    List<ImageUploadResult> uploadImages(List<MultipartFile> files, String folder);
    ImageUploadResult replaceImage(String oldPublicId, MultipartFile newFile, String folder);
}
