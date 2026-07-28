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
@Table(name = "spec_units")
public class SpecUnit {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String value; // e.g. "150Ah", "12V", "36 Months"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spec_attribute_id", nullable = false)
    private SpecAttribute specAttribute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spec_category_id", nullable = false)
    private SpecCategory specCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
