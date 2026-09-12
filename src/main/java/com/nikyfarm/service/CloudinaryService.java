package com.nikyfarm.service;

import com.nikyfarm.dto.ImageUploadResult;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    ImageUploadResult uploadImage(MultipartFile file, String folder);

    void deleteImage(String publicId);

    List<ImageUploadResult> uploadImages(List<MultipartFile> files, String folder);

    ImageUploadResult replaceImage(String oldPublicId, MultipartFile newFile, String folder);
}
