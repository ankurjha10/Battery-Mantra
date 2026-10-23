package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity 
@Table(name = "vehicle")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vehicleId;

    private String model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuel_id")
    private Fuel fuel;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    private String imageUrl;

    private String capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "slug")
    private String slug;

    public String getSlug() {
        if (slug != null && !slug.trim().isEmpty()) {
            return slug;
        }
        String make = manufacturer != null ? manufacturer.getName() : "";
        String modelName = model != null ? model : "";
        return (make + "-" + modelName)
                .toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

}


