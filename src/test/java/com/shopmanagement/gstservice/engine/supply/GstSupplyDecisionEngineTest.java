package com.shopmanagement.gstservice.engine.supply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class GstSupplyDecisionEngineTest {

    private final GstSupplyDecisionEngine engine = new GstSupplyDecisionEngine();

    @Test
    void physicianSampleReversesTracedItcAndChargesNoOutputGst() {
        SupplyDecision decision = engine.decide(unrelated(new SupplyFacts())
                .transactionDate(LocalDate.of(2026, 10, 19))
                .issueType("SAMPLE")
                .recipientType("DOCTOR")
                .gstPercent(5.0)
                .hsn("3004")
                .purchaseTaxTraced(true)
                .attributableCgst(new BigDecimal("6.00"))
                .attributableSgst(new BigDecimal("6.00"))
                .cost(new BigDecimal("100.00")));

        assertEquals(GstSupplyDecisionEngine.PHYSICIAN_SAMPLE, decision.classification());
        assertFalse(decision.supplyExists());
        assertFalse(decision.outputGstApplicable());
        assertEquals(SupplyDecision.ITC_REVERSAL, decision.itcTreatment());
        assertEquals(new BigDecimal("6.00"), decision.itcCgst());
        assertEquals(new BigDecimal("6.00"), decision.itcSgst());
        assertEquals("SAMPLE_ISSUE_NOTE", decision.documentType());
        assertFalse(decision.portalReturnReady());
        assertTrue(decision.legalReference().contains("92/11/2019"));
    }

    @Test
    void missingPurchaseTaxKeepsReversalPending() {
        SupplyDecision decision = engine.decide(unrelated(new SupplyFacts())
                .issueType("SAMPLE")
                .recipientType("DOCTOR")
                .purchaseTaxTraced(false));
        assertEquals(SupplyDecision.ITC_PENDING, decision.itcTreatment());
        assertEquals(BigDecimal.ZERO.setScale(2), decision.itcCgst());
    }

    @Test
    void unknownRelationshipDoesNotAssumeAFreeSample() {
        SupplyDecision decision = engine.decide(new SupplyFacts()
                .issueType("SAMPLE")
                .recipientType("DOCTOR"));
        assertEquals(SupplyDecision.REVIEW, decision.status());
        assertEquals(SupplyDecision.ITC_PENDING, decision.itcTreatment());
        assertFalse(decision.itcReversalRequired());
    }

    @Test
    void mrCustodyDoesNotRecogniseExpenseOrItcReversal() {
        SupplyDecision decision = engine.decide(new SupplyFacts()
                .issueType("FIELD_STOCK")
                .recipientType("MR")
                .purchaseTaxTraced(true)
                .attributableCgst(new BigDecimal("6")));
        assertEquals(GstSupplyDecisionEngine.MR_STOCK_CUSTODY, decision.classification());
        assertEquals(SupplyDecision.ITC_NA, decision.itcTreatment());
        assertEquals("STOCK_CUSTODY_ONLY", decision.accountingTreatment());
        assertFalse(decision.outputGstApplicable());
    }

    @Test
    void distributionFromCustodyToDoctorUsesSampleRules() {
        SupplyDecision decision = engine.decide(unrelated(new SupplyFacts())
                .requestedClassification(GstSupplyDecisionEngine.MR_DISTRIBUTION)
                .recipientType("DOCTOR")
                .purchaseTaxTraced(true)
                .attributableIgst(new BigDecimal("12.00"))
                .sellerStateCode("27")
                .recipientStateCode("29"));
        assertEquals(GstSupplyDecisionEngine.PHYSICIAN_SAMPLE, decision.classification());
        assertEquals(new BigDecimal("12.00"), decision.itcIgst());
    }

    @Test
    void retailerFreePromotionIsNotASale() {
        SupplyDecision decision = engine.decide(unrelated(new SupplyFacts())
                .issueType("PROMOTIONAL")
                .recipientType("PHARMACY")
                .gstPercent(5.0)
                .purchaseTaxTraced(true)
                .attributableCgst(new BigDecimal("2.50"))
                .attributableSgst(new BigDecimal("2.50")));
        assertEquals(GstSupplyDecisionEngine.RETAILER_FREE_PROMOTION, decision.classification());
        assertFalse(decision.supplyExists());
        assertEquals(SupplyDecision.ITC_REVERSAL, decision.itcTreatment());
    }

    @Test
    void buyTenGetOneTaxesTheChargedPriceAtTheProductRate() {
        SupplyDecision decision = engine.decide(saleStates(new SupplyFacts())
                .requestedClassification(GstSupplyDecisionEngine.BUY_X_GET_Y)
                .paidQuantity(new BigDecimal("10"))
                .freeQuantity(new BigDecimal("1"))
                .chargedAmount(new BigDecimal("1000.00"))
                .gstPercent(5.0)
                .hsn("3004"));
        assertTrue(decision.supplyExists());
        assertEquals(new BigDecimal("1000.00"), decision.taxableValue());
        assertEquals(5.0, decision.gstPercent());
        assertEquals(new BigDecimal("25.00"), decision.cgst());
        assertEquals(new BigDecimal("25.00"), decision.sgst());
        assertEquals(SupplyDecision.ITC_RETAINED, decision.itcTreatment());
        assertEquals("CIRC92_B_BUY_X_GET_Y", decision.ruleCode());
    }

    @Test
    void differentRateFreeProductUsesMixedSupplyRate() {
        SupplyDecision decision = engine.decide(saleStates(new SupplyFacts())
                .paidQuantity(new BigDecimal("10"))
                .freeQuantity(new BigDecimal("1"))
                .chargedAmount(new BigDecimal("1000.00"))
                .gstPercent(5.0)
                .freeProductGstPercent(12.0)
                .hsn("3004")
                .freeProductHsn("9018"));
        assertEquals("SEC_8_MIXED", decision.ruleCode());
        assertEquals(12.0, decision.gstPercent());
        assertEquals(new BigDecimal("60.00"), decision.cgst());
        assertEquals(new BigDecimal("60.00"), decision.sgst());
    }

    @Test
    void compositeSupplyKeepsThePrincipalRate() {
        SupplyDecision decision = engine.decide(saleStates(new SupplyFacts())
                .paidQuantity(new BigDecimal("1"))
                .freeQuantity(new BigDecimal("1"))
                .chargedAmount(new BigDecimal("100.00"))
                .gstPercent(5.0)
                .freeProductGstPercent(18.0)
                .compositeSupply(true));
        assertEquals(5.0, decision.gstPercent());
        assertEquals("CIRC92_B_BUY_X_GET_Y", decision.ruleCode());
    }

    @Test
    void invoiceDiscountReducesTaxableValue() {
        SupplyDecision decision = engine.decide(saleStates(new SupplyFacts())
                .invoiceDiscount(true)
                .chargedAmount(new BigDecimal("900.00"))
                .discountAmount(new BigDecimal("100.00"))
                .gstPercent(18.0));
        assertEquals(GstSupplyDecisionEngine.INVOICE_DISCOUNT, decision.classification());
        assertEquals(new BigDecimal("900.00"), decision.taxableValue());
        assertEquals(new BigDecimal("81.00"), decision.cgst());
        assertEquals(SupplyDecision.ITC_RETAINED, decision.itcTreatment());
    }

    @Test
    void qualifyingPostSaleDiscountBeforeOctober2025ReducesTax() {
        SupplyDecision decision = engine.decide(saleStates(new SupplyFacts())
                .transactionDate(LocalDate.of(2025, 9, 30))
                .postSaleDiscount(true)
                .priorAgreement(true)
                .linkedToInvoice(true)
                .recipientItcReversed(true)
                .discountAmount(new BigDecimal("100.00"))
                .gstPercent(12.0));
        assertEquals("GST_CREDIT_NOTE", decision.documentType());
        assertEquals(new BigDecimal("6.00"), decision.cgst());
        assertEquals(new BigDecimal("6.00"), decision.sgst());
    }

    @Test
    void incompletePostSaleDiscountIsACommercialCreditNote() {
        SupplyDecision decision = engine.decide(saleStates(new SupplyFacts())
                .transactionDate(LocalDate.of(2025, 6, 1))
                .postSaleDiscount(true)
                .discountAmount(new BigDecimal("100.00"))
                .gstPercent(12.0));
        assertEquals("COMMERCIAL_CREDIT_NOTE", decision.documentType());
        assertFalse(decision.outputGstApplicable());
        assertEquals(BigDecimal.ZERO.setScale(2), decision.cgst());
    }

    @Test
    void postSaleDiscountAfterOctober2025RequiresActivation() {
        SupplyDecision decision = engine.decide(saleStates(new SupplyFacts())
                .transactionDate(LocalDate.of(2026, 1, 15))
                .postSaleDiscount(true)
                .linkedToInvoice(true)
                .discountAmount(new BigDecimal("100"))
                .gstPercent(5.0));
        assertEquals(SupplyDecision.REVIEW, decision.status());
        assertFalse(decision.outputGstApplicable());
    }

    @Test
    void commercialCreditNoteAfterOctober2025DoesNotChangeGst() {
        SupplyDecision decision = engine.decide(saleStates(new SupplyFacts())
                .transactionDate(LocalDate.of(2026, 1, 15))
                .postSaleDiscount(true)
                .commercialCreditNote(true)
                .discountAmount(new BigDecimal("50"))
                .gstPercent(5.0));
        assertEquals(SupplyDecision.DETERMINED, decision.status());
        assertEquals("COMMERCIAL_CREDIT_NOTE", decision.documentType());
        assertEquals(BigDecimal.ZERO.setScale(2), decision.taxableValue());
    }

    @Test
    void relatedPartyWithoutConsiderationIsASupplyAtOpenMarketValue() {
        SupplyDecision decision = engine.decide(new SupplyFacts()
                .relatedParty(true)
                .distinctRegistration(false)
                .openMarketValue(new BigDecimal("200.00"))
                .gstPercent(5.0)
                .sellerStateCode("27")
                .recipientStateCode("27"));
        assertEquals(GstSupplyDecisionEngine.RELATED_PARTY_SUPPLY, decision.classification());
        assertTrue(decision.supplyExists());
        assertEquals(new BigDecimal("200.00"), decision.taxableValue());
        assertEquals(SupplyDecision.ITC_RETAINED, decision.itcTreatment());
        assertEquals("RULE_28_OPEN_MARKET", decision.valuationMethod());
    }

    @Test
    void distinctRegistrationUsesInvoiceValueWhenRecipientHasFullItc() {
        SupplyDecision decision = engine.decide(new SupplyFacts()
                .distinctRegistration(true)
                .recipientFullItc(true)
                .declaredInvoiceValue(new BigDecimal("150.00"))
                .openMarketValue(new BigDecimal("400.00"))
                .gstPercent(18.0)
                .sellerStateCode("27")
                .recipientStateCode("29"));
        assertEquals(GstSupplyDecisionEngine.DISTINCT_REGISTRATION_TRANSFER, decision.classification());
        assertEquals("RULE_28_INVOICE_DEEMED_OMV", decision.valuationMethod());
        assertEquals(new BigDecimal("150.00"), decision.taxableValue());
        assertTrue(decision.interState());
        assertEquals(new BigDecimal("27.00"), decision.igst());
        assertEquals(BigDecimal.ZERO.setScale(2), decision.cgst());
    }

    @Test
    void distinctRegistrationFallsBackToCostPlusTen() {
        SupplyDecision decision = engine.decide(new SupplyFacts()
                .distinctRegistration(true)
                .cost(new BigDecimal("100.00"))
                .gstPercent(5.0)
                .sellerStateCode("09")
                .recipientStateCode("09"));
        assertEquals(new BigDecimal("110.00"), decision.taxableValue());
        assertEquals("RULE_30_COST_PLUS_10", decision.valuationMethod());
        assertEquals(new BigDecimal("2.75"), decision.cgst());
        assertEquals(new BigDecimal("2.75"), decision.sgst());
    }

    @Test
    void sameRegistrationTransferIsNotTaxable() {
        SupplyDecision decision = engine.decide(new SupplyFacts().sameRegistrationTransfer(true).gstPercent(18.0));
        assertEquals(GstSupplyDecisionEngine.SAME_REGISTRATION_TRANSFER, decision.classification());
        assertFalse(decision.supplyExists());
        assertEquals(SupplyDecision.ITC_NA, decision.itcTreatment());
        assertEquals("DELIVERY_CHALLAN", decision.documentType());
    }

    @Test
    void nilFiveTwelveAndEighteenRatesFollowTheProduct() {
        assertEquals(0.0, rateDecision(0.0).gstPercent());
        assertEquals(new BigDecimal("2.50"), rateDecision(5.0).cgst());
        assertEquals(new BigDecimal("6.00"), rateDecision(12.0).cgst());
        assertEquals(new BigDecimal("9.00"), rateDecision(18.0).cgst());
    }

    @Test
    void employeeGiftInsideBothCapsIsNotASupply() {
        SupplyDecision decision = engine.decide(unrelated(new SupplyFacts())
                .requestedClassification(GstSupplyDecisionEngine.MR_PERSONAL_GIFT)
                .employerEmployee(true)
                .fyGiftCostToDate(new BigDecimal("1000"))
                .fyGiftOmvToDate(new BigDecimal("1200"))
                .thisGiftCost(new BigDecimal("100"))
                .thisGiftOmv(new BigDecimal("150"))
                .purchaseTaxTraced(true)
                .attributableCgst(new BigDecimal("2.50"))
                .attributableSgst(new BigDecimal("2.50")));
        assertFalse(decision.supplyExists());
        assertEquals(SupplyDecision.ITC_REVERSAL, decision.itcTreatment());
    }

    @Test
    void employeeGiftNearTheCapRequiresReview() {
        SupplyDecision decision = engine.decide(new SupplyFacts()
                .requestedClassification(GstSupplyDecisionEngine.MR_PERSONAL_GIFT)
                .employerEmployee(true)
                .fyGiftCostToDate(new BigDecimal("49000"))
                .fyGiftOmvToDate(new BigDecimal("1000"))
                .thisGiftCost(new BigDecimal("2000"))
                .thisGiftOmv(new BigDecimal("2000")));
        assertEquals(SupplyDecision.REVIEW, decision.status());
        assertFalse(decision.outputGstApplicable());
    }

    private static SupplyDecision rateDecision(double percent) {
        return new GstSupplyDecisionEngine().decide(saleStates(new SupplyFacts())
                .paidQuantity(BigDecimal.TEN)
                .freeQuantity(BigDecimal.ONE)
                .chargedAmount(new BigDecimal("100.00"))
                .gstPercent(percent));
    }

    private static SupplyFacts unrelated(SupplyFacts facts) {
        return facts.relatedParty(false).distinctRegistration(false);
    }

    private static SupplyFacts saleStates(SupplyFacts facts) {
        return facts.sellerStateCode("27").recipientStateCode("27");
    }
}
