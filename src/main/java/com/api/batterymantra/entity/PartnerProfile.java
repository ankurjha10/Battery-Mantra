package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "partner_profiles")
public class PartnerProfile {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String businessName;
    private String contactPerson;

    private String alternatePhone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @ManyToMany
    @JoinTable(
        name = "partner_cities",
        joinColumns = @JoinColumn(name = "partner_id"),
        inverseJoinColumns = @JoinColumn(name = "city_id")
    )
    @Builder.Default
    private Set<City> operatingCities = new HashSet<>();

    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
