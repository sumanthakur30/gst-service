package com.shopmanagement.gstservice.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "eway_part_b_updates")
@Getter
@Setter
public class EwayPartBUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "eway_bill_request_id", nullable = false)
    private Long ewayBillRequestId;

    @Column(name = "ewb_no", length = 40)
    private String ewbNo;

    @Column(name = "vehicle_no", length = 40)
    private String vehicleNo;

    @Column(name = "from_place", length = 120)
    private String fromPlace;

    @Column(name = "trans_doc_no", length = 80)
    private String transDocNo;

    @Column(length = 40)
    private String provider;

    @Column(name = "provider_status", length = 40)
    private String providerStatus;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
