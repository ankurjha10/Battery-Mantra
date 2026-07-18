package com.api.batterymantra.controller;

import com.api.batterymantra.dto.upload.UploadResponse;
import com.api.batterymantra.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder) throws IOException {

        Map<String, String> result = cloudinaryService.upload(file, folder);

        UploadResponse response = new UploadResponse(
                result.get("secure_url"),
                result.get("public_id")
        );

        return ResponseEntity.ok(response);
    }
}
