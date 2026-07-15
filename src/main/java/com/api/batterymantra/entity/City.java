package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cities")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class City {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID cityId;

    @Column(nullable = false, unique = true)
    private String cityName;

    @Column(nullable = false)
    private String stateName;

    @Column(nullable = true)
    private String cityImage;

    @Column(nullable = false)
    private Boolean isPopular = false;

    @Column(nullable = false)
    private Boolean isCodAvailable = false;

    @Column(nullable = false)
    private Boolean isExchangeAvailable = false;

    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pincode> pincodes = new ArrayList<>();
}
