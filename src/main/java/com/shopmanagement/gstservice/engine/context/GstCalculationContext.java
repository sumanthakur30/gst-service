package com.shopmanagement.gstservice.engine.context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shopmanagement.gstservice.model.BusinessType;
import com.shopmanagement.gstservice.model.CustomerGstType;
import com.shopmanagement.gstservice.model.PricingMode;

public class GstCalculationContext {

    private final boolean applyGst;
    private final BusinessType businessType;
    private final CustomerGstType customerGstType;
    private final PricingMode pricingMode;
    private final String sellerStateCode;
    private final String customerStateCode;
    private final String sellerGstin;
    private final String customerGstin;
    private final LocalDate transactionDate;
    private final boolean reverseCharge;
    private final boolean discountBeforeTax;
    private final Map<String, String> businessAttributes;
    private final List<GstLineContext> lines = new ArrayList<>();

    private BigDecimal headerDiscount = BigDecimal.ZERO;
    private BigDecimal invoiceLevelDiscount = BigDecimal.ZERO;
    private BigDecimal incomingTaxAmount;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal taxableAmount = BigDecimal.ZERO;
    private BigDecimal totalTax = BigDecimal.ZERO;
    private BigDecimal totalCess = BigDecimal.ZERO;
    private BigDecimal cgst = BigDecimal.ZERO;
    private BigDecimal sgst = BigDecimal.ZERO;
    private BigDecimal igst = BigDecimal.ZERO;
    private BigDecimal roundOff = BigDecimal.ZERO;
    private BigDecimal grandTotal = BigDecimal.ZERO;
    private boolean interState;
    private boolean gstEnabled;

    public GstCalculationContext(
            boolean applyGst,
            BusinessType businessType,
            CustomerGstType customerGstType,
            PricingMode pricingMode,
            String sellerStateCode,
            String customerStateCode,
            String sellerGstin,
            String customerGstin,
            LocalDate transactionDate,
            boolean reverseCharge,
            boolean discountBeforeTax,
            Map<String, String> businessAttributes) {
        this.applyGst = applyGst;
        this.businessType = businessType == null ? BusinessType.DEFAULT : businessType;
        this.customerGstType = customerGstType == null ? CustomerGstType.UNREGISTERED : customerGstType;
        this.pricingMode = pricingMode == null ? PricingMode.INCLUSIVE : pricingMode;
        this.sellerStateCode = sellerStateCode;
        this.customerStateCode = customerStateCode;
        this.sellerGstin = sellerGstin;
        this.customerGstin = customerGstin;
        this.transactionDate = transactionDate == null ? LocalDate.now() : transactionDate;
        this.reverseCharge = reverseCharge;
        this.discountBeforeTax = discountBeforeTax;
        this.businessAttributes = businessAttributes == null ? new HashMap<>() : new HashMap<>(businessAttributes);
    }

    public void addLine(GstLineContext line) {
        lines.add(line);
    }

    public List<GstLineContext> lines() {
        return lines;
    }

    public boolean applyGst() {
        return applyGst;
    }

    public BusinessType businessType() {
        return businessType;
    }

    public CustomerGstType customerGstType() {
        return customerGstType;
    }

    public PricingMode pricingMode() {
        return pricingMode;
    }

    public String sellerStateCode() {
        return sellerStateCode;
    }

    public String customerStateCode() {
        return customerStateCode;
    }

    public String sellerGstin() {
        return sellerGstin;
    }

    public String customerGstin() {
        return customerGstin;
    }

    public LocalDate transactionDate() {
        return transactionDate;
    }

    public boolean reverseCharge() {
        return reverseCharge;
    }

    public boolean discountBeforeTax() {
        return discountBeforeTax;
    }

    public Map<String, String> businessAttributes() {
        return businessAttributes;
    }

    public BigDecimal headerDiscount() {
        return headerDiscount;
    }

    public void setHeaderDiscount(BigDecimal headerDiscount) {
        this.headerDiscount = headerDiscount;
    }

    public BigDecimal invoiceLevelDiscount() {
        return invoiceLevelDiscount;
    }

    public void setInvoiceLevelDiscount(BigDecimal invoiceLevelDiscount) {
        this.invoiceLevelDiscount = invoiceLevelDiscount;
    }

    public BigDecimal incomingTaxAmount() {
        return incomingTaxAmount;
    }

    public void setIncomingTaxAmount(BigDecimal incomingTaxAmount) {
        this.incomingTaxAmount = incomingTaxAmount;
    }

    public BigDecimal subtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal totalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public BigDecimal taxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal totalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal totalCess() {
        return totalCess;
    }

    public void setTotalCess(BigDecimal totalCess) {
        this.totalCess = totalCess;
    }

    public BigDecimal cgst() {
        return cgst;
    }

    public void setCgst(BigDecimal cgst) {
        this.cgst = cgst;
    }

    public BigDecimal sgst() {
        return sgst;
    }

    public void setSgst(BigDecimal sgst) {
        this.sgst = sgst;
    }

    public BigDecimal igst() {
        return igst;
    }

    public void setIgst(BigDecimal igst) {
        this.igst = igst;
    }

    public BigDecimal roundOff() {
        return roundOff;
    }

    public void setRoundOff(BigDecimal roundOff) {
        this.roundOff = roundOff;
    }

    public BigDecimal grandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public boolean interState() {
        return interState;
    }

    public void setInterState(boolean interState) {
        this.interState = interState;
    }

    public boolean gstEnabled() {
        return gstEnabled;
    }

    public void setGstEnabled(boolean gstEnabled) {
        this.gstEnabled = gstEnabled;
    }

    public boolean isInterStateSupply() {
        return customerStateCode != null
                && !customerStateCode.isBlank()
                && sellerStateCode != null
                && !sellerStateCode.isBlank()
                && !customerStateCode.trim().equals(sellerStateCode.trim());
    }
}
