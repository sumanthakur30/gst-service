package com.shopmanagement.gstservice.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gst_supply_rule")
@Getter
@Setter
public class GstSupplyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String ruleCode;

    @Column(nullable = false, length = 800)
    private String legalReference;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(nullable = false, length = 40)
    private String outcomeStatus;

    @Column(nullable = false)
    private boolean activated = true;
}
