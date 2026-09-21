package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "instagram_reel")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InstagramReel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reelId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private Boolean isActive = true;

    private Integer displayOrder;
}
