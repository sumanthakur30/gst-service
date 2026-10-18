package com.shopmanagement.gstservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "gst_supply_decision",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "shop_id", "source_type", "source_id"}))
@Getter
@Setter
public class GstSupplyDecisionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 64)
    private String shopId;

    @Column(nullable = false, length = 40)
    private String sourceType;

    @Column(nullable = false, length = 80)
    private String sourceId;

    @Column(nullable = false, length = 60)
    private String classification;

    @Column(nullable = false, length = 80)
    private String ruleCode;

    @Column(nullable = false, length = 800)
    private String legalReference;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(nullable = false)
    private boolean supplyExists;

    @Column(nullable = false)
    private boolean outputGstApplicable;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal taxableValue;

    @Column(length = 60)
    private String valuationMethod;

    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal gstPercent;

    @Column(nullable = false)
    private boolean interState;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal cgst;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal sgst;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal igst;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal cess;

    @Column(nullable = false, length = 40)
    private String itcTreatment;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal itcCgst;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal itcSgst;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal itcIgst;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal itcCess;

    @Column(nullable = false, length = 40)
    private String documentType;

    @Column(nullable = false, length = 80)
    private String accountingTreatment;

    @Column(nullable = false, length = 40)
    private String booksClassification;

    @Column(nullable = false)
    private boolean portalReturnReady;

    @Column(nullable = false, length = 2000)
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String inputsJson;

    @Column(length = 120)
    private String overrideBy;

    @Column(length = 500)
    private String overrideReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
