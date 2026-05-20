package com.shopmanagement.gstservice.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tax_line_snapshot")
@Getter
@Setter
public class TaxLineSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_document_snapshot_id", nullable = false)
    private TaxDocumentSnapshot taxDocumentSnapshot;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    private Long productId;

    @Column(length = 300)
    private String description;

    @Column(length = 20)
    private String hsnSac;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableValue;

    @Column(precision = 6, scale = 3)
    private BigDecimal gstRatePercent;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal igst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal cess = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(nullable = false)
    private boolean taxInclusive = false;
}
