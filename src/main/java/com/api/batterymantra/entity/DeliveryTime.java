package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "delivery_times", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"category_id", "city_id"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeliveryTime {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "days", length = 20)
    private String days;

    @Column(name = "hours", length = 20)
    private String hours;
}
