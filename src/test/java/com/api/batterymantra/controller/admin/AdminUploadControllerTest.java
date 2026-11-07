package com.api.batterymantra.controller.admin;

import com.api.batterymantra.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUploadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AdminUploadController adminUploadController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUploadController).build();
    }

    @Test
    @DisplayName("POST /api/admin/upload returns 200 OK with url and publicId on successful upload")
    void testUploadImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                MediaType.IMAGE_PNG_VALUE,
                "dummy binary".getBytes()
        );

        when(fileStorageService.uploadImage(any(), eq("products")))
                .thenReturn(new FileStorageService.UploadResult(
                        "https://batterymantra.com/uploads/products/12345.png",
                        "products/12345.png"
                ));

        mockMvc.perform(multipart("/api/admin/upload")
                        .file(file)
                        .param("folder", "products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://batterymantra.com/uploads/products/12345.png"))
                .andExpect(jsonPath("$.publicId").value("products/12345.png"));
    }

    @Test
    @DisplayName("POST /api/admin/upload returns 400 Bad Request when validation fails")
    void testUploadImage_BadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.exe",
                "application/octet-stream",
                "dummy binary".getBytes()
        );

        when(fileStorageService.uploadImage(any(), any()))
                .thenThrow(new IllegalArgumentException("Unsupported file type"));

        mockMvc.perform(multipart("/api/admin/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type"));
    }

    @Test
    @DisplayName("POST /api/admin/upload returns 500 Internal Server Error when IO fails")
    void testUploadImage_InternalServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                "dummy binary".getBytes()
        );

        when(fileStorageService.uploadImage(any(), any()))
                .thenThrow(new IOException("Disk write failure"));

        mockMvc.perform(multipart("/api/admin/upload")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("File upload failed: Disk write failure"));
    }
}

