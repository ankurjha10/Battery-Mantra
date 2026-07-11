package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.CallbackStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "callback_request")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CallbackRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long callbackId;

    @Column(nullable = false)
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallbackStatus status = CallbackStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
