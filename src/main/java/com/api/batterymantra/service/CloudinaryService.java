package com.api.batterymantra.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/svg+xml"
    );

    /**
     * Uploads an image file to Cloudinary.
     *
     * @param file   the multipart file to upload
     * @param folder the Cloudinary folder path (e.g. "battery-mantra/products")
     * @return a map containing "secure_url" and "public_id" from the upload result
     * @throws IllegalArgumentException if the file is empty, too large, or has an invalid type
     * @throws IOException              if the upload fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> upload(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed types: PNG, JPEG, WebP, SVG"
            );
        }

        log.info("Uploading file '{}' ({} bytes) to folder '{}'",
                file.getOriginalFilename(), file.getSize(), folder);

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image"
                )
        );

        String secureUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        log.info("Upload successful. publicId='{}', url='{}'", publicId, secureUrl);

        return Map.of(
                "secure_url", secureUrl,
                "public_id", publicId
        );
    }
}
