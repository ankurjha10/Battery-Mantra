package com.api.batterymantra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "battery_capacities")
public class BatteryCapacity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "capacity_id", updatable = false, nullable = false)
    private UUID capacityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotBlank
    @Column(name = "capacity_name", nullable = false, length = 100)
    private String capacityName;
}
