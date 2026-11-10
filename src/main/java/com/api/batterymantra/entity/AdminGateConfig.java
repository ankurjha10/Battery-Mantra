package com.api.batterymantra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_gate_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminGateConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Builder.Default
    private Long id = SINGLETON_ID;

    @Column(name = "admin_pin_hash")
    private String adminPinHash;

    @Column(name = "sub_admin_pin_hash")
    private String subAdminPinHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

