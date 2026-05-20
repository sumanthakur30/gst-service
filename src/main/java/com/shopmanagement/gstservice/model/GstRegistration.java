package com.shopmanagement.gstservice.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gst_registration")
@Getter
@Setter
public class GstRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 300)
    private String legalName;

    @Column(nullable = false, length = 15)
    private String gstin;

    @Column(nullable = false, length = 2)
    private String stateCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegistrationType registrationType = RegistrationType.REGULAR;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    private LocalDate effectiveTo;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
