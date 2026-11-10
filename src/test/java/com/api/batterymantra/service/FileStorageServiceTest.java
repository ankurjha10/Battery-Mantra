package com.api.batterymantra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private FileStorageService fileStorageService;
    private final String baseUrl = "https://batterymantra.com/uploads";

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempUploadDir.toString(), baseUrl);
    }

    @Test
    @DisplayName("uploadImage: successfully saves image to local disk and returns public URL and publicId")
    void testUploadImage_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample-battery.png",
                "image/png",
                "fake image binary content".getBytes()
        );

        FileStorageService.UploadResult result = fileStorageService.uploadImage(file, "products");

        assertNotNull(result);
        assertNotNull(result.url());
        assertNotNull(result.publicId());

        assertTrue(result.publicId().startsWith("products/"));
        assertTrue(result.publicId().endsWith(".png"));
        assertEquals("https://batterymantra.com/uploads/" + result.publicId(), result.url());

        // Verify the file was actually written to the local disk directory
        Path savedFile = tempUploadDir.resolve(result.publicId());
        assertTrue(Files.exists(savedFile));
        assertEquals("fake image binary content", Files.readString(savedFile));
    }

    @Test
    @DisplayName("uploadImage: supports various allowed image extensions")
    void testUploadImage_SupportedExtensions() throws IOException {
        String[] extensions = {"jpg", "jpeg", "webp", "svg", "gif"};

        for (String ext : extensions) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-image." + ext,
                    "image/" + ext,
                    ("content-" + ext).getBytes()
            );

            FileStorageService.UploadResult result = fileStorageService.uploadImage(file, "categories");
            assertTrue(result.publicId().endsWith("." + ext));
            assertTrue(Files.exists(tempUploadDir.resolve(result.publicId())));
        }
    }

    @Test
    @DisplayName("uploadImage: throws IllegalArgumentException for empty or null file")
    void testUploadImage_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.uploadImage(emptyFile, "misc")
        );
        assertEquals("Cannot upload an empty file", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.uploadImage(null, "misc")
        );
        assertEquals("Cannot upload an empty file", ex2.getMessage());
    }

    @Test
    @DisplayName("uploadImage: throws IllegalArgumentException for unsupported extensions")
    void testUploadImage_UnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.sh",
                "application/x-sh",
                "echo hack".getBytes()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.uploadImage(file, "misc")
        );
        assertTrue(ex.getMessage().contains("Unsupported file type"));
    }

    @Test
    @DisplayName("uploadImage: sanitizes folder name and prevents directory traversal while allowing safe slashes")
    void testUploadImage_SanitizeFolder() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "data".getBytes()
        );

        // Path traversal attempts in folder name with special characters
        FileStorageService.UploadResult result = fileStorageService.uploadImage(file, "../../dangerous/folder!@#$");

        // Allowed chars: [a-zA-Z0-9_/-] with .. stripped -> "dangerous/folder"
        assertFalse(result.publicId().contains(".."));
        assertTrue(result.publicId().startsWith("dangerous/folder/"));

        // Blank or null folder defaults to "misc"
        FileStorageService.UploadResult miscResult = fileStorageService.uploadImage(file, "   ");
        assertTrue(miscResult.publicId().startsWith("misc/"));
    }

    @Test
    @DisplayName("uploadImage: preserves nested subdirectories (e.g. battery-mantra/categories)")
    void testUploadImage_NestedSubfolders() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "category.png",
                "image/png",
                "category data".getBytes()
        );

        FileStorageService.UploadResult result = fileStorageService.uploadImage(file, "battery-mantra/categories");
        assertNotNull(result);
        assertTrue(result.publicId().startsWith("battery-mantra/categories/"));
        assertTrue(result.publicId().endsWith(".png"));
        assertEquals("https://batterymantra.com/uploads/" + result.publicId(), result.url());

        // Verify the nested directory structure was physically created on disk
        Path savedFile = tempUploadDir.resolve(result.publicId());
        assertTrue(Files.exists(savedFile));
        assertEquals("category data", Files.readString(savedFile));
    }

    @Test
    @DisplayName("uploadPartnerImage: allows png, jpg, jpeg, webp and rejects svg/gif and >15MB")
    void testUploadPartnerImage_Validation() throws IOException {
        String[] allowedExts = {"png", "jpg", "jpeg", "webp"};
        String[] allowedTypes = {"image/png", "image/jpeg", "image/jpeg", "image/webp"};

        for (int i = 0; i < allowedExts.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "battery." + allowedExts[i],
                    allowedTypes[i],
                    ("content-" + i).getBytes()
            );
            FileStorageService.UploadResult result = fileStorageService.uploadPartnerImage(file, "products");
            assertTrue(result.publicId().startsWith("products/"));
            assertTrue(result.publicId().endsWith("." + allowedExts[i]));
        }

        MockMultipartFile svgFile = new MockMultipartFile(
                "file", "icon.svg", "image/svg+xml", "<svg/>".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.uploadPartnerImage(svgFile, "products")
        );

        MockMultipartFile oversized = new MockMultipartFile(
                "file", "huge.jpg", "image/jpeg", new byte[(15 * 1024 * 1024) + 1]
        );
        IllegalArgumentException sizeEx = assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.uploadPartnerImage(oversized, "products")
        );
        assertTrue(sizeEx.getMessage().contains("15MB"));
    }
}

