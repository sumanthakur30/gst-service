package com.shopmanagement.gstservice.engine.supply;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SupplyDecision(
        String classification,
        String ruleCode,
        String legalReference,
        LocalDate effectiveFrom,
        String status,
        boolean supplyExists,
        boolean outputGstApplicable,
        BigDecimal taxableValue,
        String valuationMethod,
        double gstPercent,
        boolean interState,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        BigDecimal cess,
        String itcTreatment,
        BigDecimal itcCgst,
        BigDecimal itcSgst,
        BigDecimal itcIgst,
        BigDecimal itcCess,
        String documentType,
        String accountingTreatment,
        String booksClassification,
        boolean portalReturnReady,
        String explanation) {

    public static final String DETERMINED = "DETERMINED";
    public static final String REVIEW = "REQUIRES_AUTHORISED_REVIEW";

    public static final String ITC_RETAINED = "RETAINED";
    public static final String ITC_REVERSAL = "REVERSAL_REQUIRED";
    public static final String ITC_PENDING = "PENDING_REVIEW";
    public static final String ITC_NA = "NOT_APPLICABLE";

    public boolean itcReversalRequired() {
        return ITC_REVERSAL.equals(itcTreatment);
    }
}
