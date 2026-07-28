package com.api.batterymantra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "spec_attributes")
public class SpecAttribute {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name; // e.g. "Capacity", "Height", "Free Replacement Period"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spec_category_id", nullable = false)
    private SpecCategory specCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
