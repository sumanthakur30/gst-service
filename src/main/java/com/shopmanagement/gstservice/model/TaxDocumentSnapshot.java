package com.shopmanagement.gstservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tax_document_snapshot")
@Getter
@Setter
public class TaxDocumentSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 40)
    private String sourceService;

    @Column(nullable = false, length = 40)
    private String sourceType;

    @Column(nullable = false, length = 64)
    private String sourceId;

    @Column(length = 64)
    private String sourceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentType documentType;

    @Column(nullable = false)
    private LocalDate documentDate;

    @Column(length = 2)
    private String placeOfSupplyState;

    @Column(name = "seller_gst_registration_id")
    private Long sellerGstRegistrationId;

    @Column(length = 15)
    private String buyerGstin;

    @Column(length = 2)
    private String buyerStateCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplyType supplyType = SupplyType.B2C;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableValue;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTax;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal igst = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal cess = BigDecimal.ZERO;

    @Column(length = 30)
    private String businessType;

    @Column(name = "calculation_snapshot_json", columnDefinition = "TEXT")
    private String calculationSnapshotJson;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Column(columnDefinition = "TEXT")
    private String taxSummaryJson;

    @Column(length = 64)
    private String snapshotHash;

    @Column(name = "original_snapshot_id")
    private Long originalSnapshotId;

    @Column(length = 120)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "taxDocumentSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<TaxLineSnapshot> lines = new java.util.ArrayList<>();
}
