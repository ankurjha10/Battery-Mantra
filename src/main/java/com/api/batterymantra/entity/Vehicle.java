package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.FuelType;
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
    private int yearFrom;
    private int yearTo;

    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    private String imageUrl;
}
