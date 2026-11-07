package com.api.batterymantra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Legacy service interface retained for backward compatibility.
 * All image uploads are now routed directly to {@link FileStorageService} (Local VPS Disk Storage).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final FileStorageService fileStorageService;

    public Map<String, String> upload(MultipartFile file, String folder) throws IOException {
        log.info("Delegating upload of '{}' in folder '{}' to FileStorageService",
                file != null ? file.getOriginalFilename() : "null", folder);

        FileStorageService.UploadResult result = fileStorageService.uploadImage(file, folder);

        return Map.of(
                "secure_url", result.url(),
                "public_id", result.publicId()
        );
    }
}
