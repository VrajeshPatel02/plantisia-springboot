package com.nikyfarm.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nikyfarm.dto.ImageUploadResult;
import com.nikyfarm.exception.ImageUploadException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES =
            List.of(
                    "image/jpeg",
                    "image/jpg",
                    "image/png",
                    "image/webp");

    private static final List<String> ALLOWED_VIDEO_CONTENT_TYPES =
            List.of(
                    "video/mp4",
                    "video/quicktime",
                    "video/webm");

    private static final long MAX_IMAGE_SIZE_BYTES =
            10L * 1024 * 1024;

    private static final long MAX_VIDEO_SIZE_BYTES =
            50L * 1024 * 1024;

    /*
     * Chunk size used for Cloudinary video uploads.
     *
     * 10 MB is appropriate for our 50 MB review-video limit.
     */
    private static final int VIDEO_CHUNK_SIZE_BYTES =
            10 * 1024 * 1024;

    private static final int MAX_FILES_PER_BATCH = 10;

    // -------------------------------------------------------------------------
    // IMAGE UPLOAD
    // -------------------------------------------------------------------------

    @Override
    public ImageUploadResult uploadImage(
            MultipartFile file,
            String folder) {

        validateImage(file);

        return upload(
                file,
                folder,
                "image",
                "image");
    }

    // -------------------------------------------------------------------------
    // VIDEO UPLOAD
    // -------------------------------------------------------------------------

    @Override
    public ImageUploadResult uploadVideo(
            MultipartFile file,
            String folder) {

        validateVideo(file);

        File temporaryFile = null;

        try {
            /*
             * Do not use MultipartFile#getBytes() for videos.
             *
             * The old implementation loaded the complete video into
             * a byte[] before sending it to Cloudinary.
             *
             * We first write the uploaded multipart file to a temporary
             * file and then give that file to Cloudinary's chunked
             * uploadLarge() API.
             */
            temporaryFile = File.createTempFile(
                    "niky-review-video-",
                    getFileExtension(file.getOriginalFilename()));

            file.transferTo(temporaryFile);

            Map<String, Object> uploadResult =
                    cloudinary
                            .uploader()
                            .uploadLarge(
                                    temporaryFile,
                                    ObjectUtils.asMap(
                                            "folder", folder,
                                            "resource_type", "video",
                                            "overwrite", false,
                                            "unique_filename", true),
                                    VIDEO_CHUNK_SIZE_BYTES);

            String fileName =
                    file.getOriginalFilename();

            String url =
                    (String) uploadResult.get("secure_url");

            String publicId =
                    (String) uploadResult.get("public_id");

            if (url == null || url.isBlank()) {
                throw new ImageUploadException(
                        "Cloudinary video upload completed but no secure URL was returned.");
            }

            if (publicId == null || publicId.isBlank()) {
                throw new ImageUploadException(
                        "Cloudinary video upload completed but no public ID was returned.");
            }

            return new ImageUploadResult(
                    fileName,
                    true,
                    url,
                    publicId,
                    null);

        } catch (IOException e) {

            throw new ImageUploadException(
                    "Failed to upload video to Cloudinary: "
                            + e.getMessage(),
                    e);

        } finally {

            /*
             * Always delete the temporary server-side file.
             *
             * If deletion fails immediately, schedule it for JVM exit.
             */
            if (temporaryFile != null
                    && temporaryFile.exists()) {

                try {

                    if (!temporaryFile.delete()) {
                        temporaryFile.deleteOnExit();
                    }

                } catch (SecurityException ignored) {

                    temporaryFile.deleteOnExit();
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // GENERIC IMAGE UPLOAD
    // -------------------------------------------------------------------------

    private ImageUploadResult upload(
            MultipartFile file,
            String folder,
            String resourceType,
            String mediaLabel) {

        String fileName =
                file.getOriginalFilename();

        try {

            Map<String, Object> uploadResult =
                    cloudinary
                            .uploader()
                            .upload(
                                    file.getBytes(),
                                    ObjectUtils.asMap(
                                            "folder", folder,
                                            "resource_type", resourceType,
                                            "overwrite", false,
                                            "unique_filename", true));

            String url =
                    (String) uploadResult.get("secure_url");

            String publicId =
                    (String) uploadResult.get("public_id");

            if (url == null || url.isBlank()) {
                throw new ImageUploadException(
                        "Cloudinary "
                                + mediaLabel
                                + " upload completed but no secure URL was returned.");
            }

            if (publicId == null || publicId.isBlank()) {
                throw new ImageUploadException(
                        "Cloudinary "
                                + mediaLabel
                                + " upload completed but no public ID was returned.");
            }

            return new ImageUploadResult(
                    fileName,
                    true,
                    url,
                    publicId,
                    null);

        } catch (IOException e) {

            throw new ImageUploadException(
                    "Failed to upload "
                            + mediaLabel
                            + " to Cloudinary: "
                            + e.getMessage(),
                    e);
        }
    }

    // -------------------------------------------------------------------------
    // MULTIPLE IMAGE UPLOAD
    // -------------------------------------------------------------------------

    @Override
    public List<ImageUploadResult> uploadImages(
            List<MultipartFile> files,
            String folder) {

        if (files == null || files.isEmpty()) {
            throw new ImageUploadException(
                    "No files provided.");
        }

        if (files.size() > MAX_FILES_PER_BATCH) {
            throw new ImageUploadException(
                    "Too many files in one request. Max "
                            + MAX_FILES_PER_BATCH
                            + " per batch.");
        }

        List<ImageUploadResult> results =
                new ArrayList<>();

        for (MultipartFile file : files) {

            String fileName =
                    file.getOriginalFilename();

            try {

                results.add(
                        uploadImage(
                                file,
                                folder));

            } catch (ImageUploadException e) {

                results.add(
                        new ImageUploadResult(
                                fileName,
                                false,
                                null,
                                null,
                                e.getMessage()));
            }
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // DELETE IMAGE
    // -------------------------------------------------------------------------

    @Override
    public void deleteImage(String publicId) {
        delete(
                publicId,
                "image");
    }

    // -------------------------------------------------------------------------
    // DELETE VIDEO
    // -------------------------------------------------------------------------

    @Override
    public void deleteVideo(String publicId) {
        delete(
                publicId,
                "video");
    }

    private void delete(
            String publicId,
            String resourceType) {

        if (publicId == null
                || publicId.isBlank()) {
            return;
        }

        try {

            cloudinary
                    .uploader()
                    .destroy(
                            publicId,
                            ObjectUtils.asMap(
                                    "resource_type",
                                    resourceType));

        } catch (IOException e) {

            throw new ImageUploadException(
                    "Failed to delete "
                            + resourceType
                            + " from Cloudinary: "
                            + e.getMessage(),
                    e);
        }
    }

    // -------------------------------------------------------------------------
    // REPLACE IMAGE
    // -------------------------------------------------------------------------

    @Override
    public ImageUploadResult replaceImage(
            String oldPublicId,
            MultipartFile newFile,
            String folder) {

        try {

            deleteImage(oldPublicId);

        } catch (ImageUploadException e) {

            /*
             * Preserve the existing behavior:
             * failure to delete the old image should not prevent
             * the replacement upload from being attempted.
             */
            System.err.println(
                    "Warning: failed to delete old image ("
                            + oldPublicId
                            + "): "
                            + e.getMessage());
        }

        return uploadImage(
                newFile,
                folder);
    }

    // -------------------------------------------------------------------------
    // VALIDATION
    // -------------------------------------------------------------------------

    private void validateImage(
            MultipartFile file) {

        validate(
                file,
                MAX_IMAGE_SIZE_BYTES,
                ALLOWED_IMAGE_CONTENT_TYPES,
                "10MB",
                "JPEG, PNG, WEBP");
    }

    private void validateVideo(
            MultipartFile file) {

        validate(
                file,
                MAX_VIDEO_SIZE_BYTES,
                ALLOWED_VIDEO_CONTENT_TYPES,
                "50MB",
                "MP4, MOV, WEBM");
    }

    private void validate(
            MultipartFile file,
            long maxSize,
            List<String> allowedTypes,
            String maxSizeLabel,
            String allowedTypesLabel) {

        if (file == null || file.isEmpty()) {

            throw new ImageUploadException(
                    "No file provided.");
        }

        if (file.getSize() > maxSize) {

            throw new ImageUploadException(
                    "File exceeds maximum size of "
                            + maxSizeLabel
                            + ".");
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !allowedTypes.contains(
                contentType.toLowerCase())) {

            throw new ImageUploadException(
                    "Unsupported content type: "
                            + contentType
                            + ". Allowed types: "
                            + allowedTypesLabel);
        }
    }

    // -------------------------------------------------------------------------
    // FILE EXTENSION
    // -------------------------------------------------------------------------

    private String getFileExtension(
            String fileName) {

        if (fileName == null
                || fileName.isBlank()) {

            return ".tmp";
        }

        int lastDot =
                fileName.lastIndexOf('.');

        if (lastDot < 0
                || lastDot == fileName.length() - 1) {

            return ".tmp";
        }

        return fileName.substring(lastDot);
    }
}