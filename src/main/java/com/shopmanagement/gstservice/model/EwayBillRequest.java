package com.shopmanagement.gstservice.model;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "eway_bill_request")
@Getter
@Setter
public class EwayBillRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tax_document_snapshot_id", nullable = false)
    private Long taxDocumentSnapshotId;

    @Column(nullable = false, length = 40)
    private String provider = "mock";

    /** PENDING | GENERATED | FAILED | CANCELLED */
    @Column(nullable = false, length = 40)
    private String status = "PENDING";

    @Column(name = "distance_km")
    private Integer distanceKm;

    @Column(name = "vehicle_no", length = 40)
    private String vehicleNo;

    @Column(name = "transporter_id", length = 40)
    private String transporterId;

    @Column(name = "transporter_name", length = 200)
    private String transporterName;

    @Column(name = "ewb_no", length = 40)
    private String ewbNo;

    @Column(name = "valid_upto")
    private LocalDateTime validUpto;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
