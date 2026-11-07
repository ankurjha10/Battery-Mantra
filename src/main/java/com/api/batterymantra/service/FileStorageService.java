package com.api.batterymantra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:/var/www/batterymantra/uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:https://batterymantra.com/uploads}")
    private String baseUrl;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "svg", "gif");

    public FileStorageService() {
    }

    public FileStorageService(String uploadDir, String baseUrl) {
        this.uploadDir = uploadDir;
        this.baseUrl = baseUrl;
    }

    public UploadResult uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }

        // 1. Sanitize folder path: allow alphanumeric, underscores, hyphens, and slashes, but prevent directory traversal (..)
        String safeFolder = "misc";
        if (folder != null && !folder.isBlank()) {
            safeFolder = folder.trim()
                    .replace("\\", "/")
                    .replaceAll("\\.{2,}", "")           // prevent directory traversal (../)
                    .replaceAll("[^a-zA-Z0-9_/-]", "")   // keep letters, numbers, _, -, /
                    .replaceAll("/+", "/")               // collapse consecutive slashes
                    .replaceAll("^/+|/+$", "");          // strip leading and trailing slashes
            if (safeFolder.isBlank()) {
                safeFolder = "misc";
            }
        }

        // 2. Extract and validate extension
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.png");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: jpg, jpeg, png, webp, svg, gif");
        }

        // 3. Generate clean unique filename: <uuid>.<ext>
        String uniqueFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? ".png" : "." + extension);

        // 4. Create directory if not exists
        Path targetFolder = Paths.get(uploadDir, safeFolder);
        if (!Files.exists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }

        // 5. Save file safely
        Path targetPath = targetFolder.resolve(uniqueFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 6. Return public URL and publicId (compatible with frontend DTO { url, publicId })
        String publicId = safeFolder + "/" + uniqueFilename;
        String publicUrl = baseUrl.replaceAll("/+$", "") + "/" + publicId;

        return new UploadResult(publicUrl, publicId);
    }

    public record UploadResult(String url, String publicId) {}
}

