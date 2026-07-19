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

    private String make;
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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String shortDescriptionDealer;

    @Embedded
    private SeoMetadata seo = new SeoMetadata();
}
