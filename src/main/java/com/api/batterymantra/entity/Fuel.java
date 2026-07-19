package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "fuels")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Fuel {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID fuelId;

    @Column(nullable = false, unique = true)
    private String fuelName;

    private Integer displayOrder = 0;
}
