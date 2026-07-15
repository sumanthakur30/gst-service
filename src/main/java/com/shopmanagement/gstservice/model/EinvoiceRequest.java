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
@Table(name = "einvoice_request")
@Getter
@Setter
public class EinvoiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tax_document_snapshot_id", nullable = false)
    private Long taxDocumentSnapshotId;

    @Column(nullable = false, length = 40)
    private String provider = "mock";

    /** PENDING | IRN_GENERATED | FAILED | CANCELLED */
    @Column(nullable = false, length = 40)
    private String status = "PENDING";

    @Column(length = 100)
    private String irn;

    @Column(name = "ack_no", length = 64)
    private String ackNo;

    @Column(name = "ack_date")
    private LocalDateTime ackDate;

    @Column(name = "signed_qr_payload", columnDefinition = "TEXT")
    private String signedQrPayload;

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
