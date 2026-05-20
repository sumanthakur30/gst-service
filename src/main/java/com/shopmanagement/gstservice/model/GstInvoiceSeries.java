package com.shopmanagement.gstservice.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gst_invoice_series")
@Getter
@Setter
public class GstInvoiceSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long gstRegistrationId;

    @Column(nullable = false, length = 9)
    private String financialYear;

    @Column(nullable = false, length = 20)
    private String seriesPrefix = "INV";

    @Column(nullable = false)
    private long lastSequence = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
