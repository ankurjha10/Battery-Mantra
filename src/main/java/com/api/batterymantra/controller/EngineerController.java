package com.api.batterymantra.controller;

import com.api.batterymantra.dto.user.CreateEngineerRequest;
import com.api.batterymantra.dto.user.EngineerResponse;
import com.api.batterymantra.service.EngineerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/engineers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EngineerController {

    private final EngineerService engineerService;

    @GetMapping
    public ResponseEntity<List<EngineerResponse>> getAllEngineers() {
        return ResponseEntity.ok(engineerService.getAllEngineers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EngineerResponse> getEngineerById(@PathVariable UUID id) {
        return ResponseEntity.ok(engineerService.getEngineerById(id));
    }

    @PostMapping
    public ResponseEntity<EngineerResponse> createEngineer(@Valid @RequestBody CreateEngineerRequest request) {
        return ResponseEntity.ok(engineerService.createEngineer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EngineerResponse> updateEngineer(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEngineerRequest request) {
        return ResponseEntity.ok(engineerService.updateEngineer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEngineer(@PathVariable UUID id) {
        engineerService.deleteEngineer(id);
        return ResponseEntity.noContent().build();
    }
}
