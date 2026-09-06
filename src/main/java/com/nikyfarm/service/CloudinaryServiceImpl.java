package com.nikyfarm.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nikyfarm.dto.ImageUploadResult;
import com.nikyfarm.exception.ImageUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService{

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    private static final int MAX_FILES_PER_BATCH = 10;
    @Override
    public ImageUploadResult uploadImage(MultipartFile file, String folder) {
        validate(file);
        String fileName = file.getOriginalFilename();
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "overwrite", false,
                            "unique_filename", true
                    )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            return new ImageUploadResult(fileName, true,url, publicId, null);
        }catch (IOException e){
            throw new ImageUploadException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }


    @Override
    public List<ImageUploadResult> uploadImages(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new ImageUploadException("No files provided.");
        }
        if (files.size() > MAX_FILES_PER_BATCH) {
            throw new ImageUploadException("Too many files in one request. Max " + MAX_FILES_PER_BATCH + " per batch.");
        }

        List<ImageUploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            try {
                results.add(uploadImage(file, folder));
            } catch (ImageUploadException e) {
                results.add(new ImageUploadResult(fileName, false, null, null, e.getMessage()));
            }
        }
        return results;
    }


    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return; // nothing to delete — e.g. old rows created before publicId was tracked
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new ImageUploadException("Failed to delete image from Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageUploadResult replaceImage(String oldPublicId, MultipartFile newFile, String folder) {
        try {
            deleteImage(oldPublicId);
        } catch (ImageUploadException e) {
            // log and continue — an orphaned old file is recoverable, losing the new upload isn't.
            // (swap this for your actual logger, e.g. log.warn(...))
            System.err.println("Warning: failed to delete old image (" + oldPublicId + "): " + e.getMessage());
        }
        return uploadImage(newFile, folder);
    }


    private void validate(MultipartFile file) {
        if(file == null || file.isEmpty()) {
            throw new ImageUploadException("No file provided.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImageUploadException("File exceeds maximum size of 10MB.");
        }

        String contentType = file.getContentType();
        if(contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ImageUploadException("Unsupported content type: "  + contentType + ".Allowed types: JPEG, PNG, WEBP");
        }
    }
}
