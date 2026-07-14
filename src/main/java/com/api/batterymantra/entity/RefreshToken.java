package com.api.batterymantra.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class RefreshToken {

    @Id
    private String refreshToken;

    private Instant expiry;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
