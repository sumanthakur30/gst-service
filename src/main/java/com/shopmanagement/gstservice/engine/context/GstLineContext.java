package com.shopmanagement.gstservice.engine.context;

import java.math.BigDecimal;

import com.shopmanagement.gstservice.model.SupplyNature;

public class GstLineContext {

    private final int lineNo;
    private final Long productId;
    private final String hsnSac;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineDiscount;
    private BigDecimal schemeDiscount;
    private BigDecimal freeQuantity;
    private Double gstPercent;
    private Double cessPercent;
    private BigDecimal mrp;
    private String batchRef;
    private String taxCategoryCode;
    private SupplyNature supplyNature = SupplyNature.TAXABLE;
    private boolean taxInclusive;
    private boolean reverseChargeLine;
    private String ruleApplied;

    private BigDecimal lineGross;
    private BigDecimal taxableValue;
    private BigDecimal taxAmount;
    private BigDecimal cessAmount;
    private BigDecimal lineTotal;
    private Double resolvedGstPercent;

    public GstLineContext(
            int lineNo,
            Long productId,
            String hsnSac,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineDiscount,
            BigDecimal schemeDiscount,
            BigDecimal freeQuantity,
            Double gstPercent,
            Double cessPercent,
            BigDecimal mrp,
            String batchRef,
            String taxCategoryCode,
            boolean taxInclusive,
            boolean reverseChargeLine) {
        this.lineNo = lineNo;
        this.productId = productId;
        this.hsnSac = hsnSac;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineDiscount = lineDiscount;
        this.schemeDiscount = schemeDiscount;
        this.freeQuantity = freeQuantity;
        this.gstPercent = gstPercent;
        this.cessPercent = cessPercent;
        this.mrp = mrp;
        this.batchRef = batchRef;
        this.taxCategoryCode = taxCategoryCode;
        this.taxInclusive = taxInclusive;
        this.reverseChargeLine = reverseChargeLine;
    }

    public int lineNo() {
        return lineNo;
    }

    public Long productId() {
        return productId;
    }

    public String hsnSac() {
        return hsnSac;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal unitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal lineDiscount() {
        return lineDiscount;
    }

    public BigDecimal schemeDiscount() {
        return schemeDiscount;
    }

    public BigDecimal freeQuantity() {
        return freeQuantity;
    }

    public Double gstPercent() {
        return gstPercent;
    }

    public void setGstPercent(Double gstPercent) {
        this.gstPercent = gstPercent;
    }

    public Double cessPercent() {
        return cessPercent;
    }

    public BigDecimal mrp() {
        return mrp;
    }

    public String batchRef() {
        return batchRef;
    }

    public String taxCategoryCode() {
        return taxCategoryCode;
    }

    public SupplyNature supplyNature() {
        return supplyNature;
    }

    public void setSupplyNature(SupplyNature supplyNature) {
        this.supplyNature = supplyNature;
    }

    public boolean taxInclusive() {
        return taxInclusive;
    }

    public void setTaxInclusive(boolean taxInclusive) {
        this.taxInclusive = taxInclusive;
    }

    public boolean reverseChargeLine() {
        return reverseChargeLine;
    }

    public String ruleApplied() {
        return ruleApplied;
    }

    public void setRuleApplied(String ruleApplied) {
        this.ruleApplied = ruleApplied;
    }

    public BigDecimal lineGross() {
        return lineGross;
    }

    public void setLineGross(BigDecimal lineGross) {
        this.lineGross = lineGross;
    }

    public BigDecimal taxableValue() {
        return taxableValue;
    }

    public void setTaxableValue(BigDecimal taxableValue) {
        this.taxableValue = taxableValue;
    }

    public BigDecimal taxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal cessAmount() {
        return cessAmount;
    }

    public void setCessAmount(BigDecimal cessAmount) {
        this.cessAmount = cessAmount;
    }

    public BigDecimal lineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public Double resolvedGstPercent() {
        return resolvedGstPercent;
    }

    public void setResolvedGstPercent(Double resolvedGstPercent) {
        this.resolvedGstPercent = resolvedGstPercent;
    }
}
