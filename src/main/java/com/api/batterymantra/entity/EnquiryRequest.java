package com.api.batterymantra.entity;

import com.api.batterymantra.entity.enums.EnquiryStatus;
import com.api.batterymantra.entity.enums.EnquiryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "enquiry_request")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EnquiryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnquiryType enquiryType;

    private String name;

    @Column(nullable = false)
    private String mobileNumber;

    private String email;

    private String companyName;

    private String gstin;

    private String quantity;

    @Column(columnDefinition = "TEXT")
    private String message;

    private UUID productId;

    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnquiryStatus status = EnquiryStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
