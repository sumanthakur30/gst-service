package com.shopmanagement.gstservice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gst_slab_master")
public class GstSlabMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "slab_code", nullable = false)
    private String slabCode;

    @Column(name = "gst_rate_percent", nullable = false)
    private BigDecimal gstRatePercent;

    @Column(name = "cess_percent", nullable = false)
    private BigDecimal cessPercent;

    @Column(name = "supply_nature", nullable = false)
    private String supplyNature;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getSlabCode() {
        return slabCode;
    }

    public double gstRatePercent() {
        return gstRatePercent == null ? 0.0 : gstRatePercent.doubleValue();
    }

    public double cessPercent() {
        return cessPercent == null ? 0.0 : cessPercent.doubleValue();
    }

    public String getSupplyNature() {
        return supplyNature;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }
}
