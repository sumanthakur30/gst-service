package com.shopmanagement.gstservice.engine.supply;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.shopmanagement.gstservice.engine.support.GstMoney;
import com.shopmanagement.gstservice.engine.support.GstTaxSplit;

/**
 * Classifies a wholesale movement and returns the GST and ITC outcome.
 * It does not accept a caller-selected tax treatment.
 */
public final class GstSupplyDecisionEngine {

    public static final String PHYSICIAN_SAMPLE = "PHYSICIAN_SAMPLE";
    public static final String MR_STOCK_CUSTODY = "MR_STOCK_CUSTODY";
    public static final String MR_DISTRIBUTION = "MR_DISTRIBUTION";
    public static final String MR_PERSONAL_GIFT = "MR_PERSONAL_GIFT";
    public static final String RETAILER_FREE_PROMOTION = "RETAILER_FREE_PROMOTION";
    public static final String BUY_X_GET_Y = "BUY_X_GET_Y";
    public static final String INVOICE_DISCOUNT = "INVOICE_DISCOUNT";
    public static final String POST_SALE_DISCOUNT = "POST_SALE_DISCOUNT";
    public static final String RELATED_PARTY_SUPPLY = "RELATED_PARTY_SUPPLY";
    public static final String DISTINCT_REGISTRATION_TRANSFER = "DISTINCT_REGISTRATION_TRANSFER";
    public static final String SAME_REGISTRATION_TRANSFER = "SAME_REGISTRATION_TRANSFER";
    public static final String OTHER_PROMOTIONAL_SUPPLY = "OTHER_PROMOTIONAL_SUPPLY";

    private static final BigDecimal EMPLOYEE_GIFT_CAP = new BigDecimal("50000.00");
    private static final LocalDate POST_SALE_AMENDMENT = LocalDate.of(2025, 10, 1);

    private final SupplyRuleCatalog catalog;

    public GstSupplyDecisionEngine() {
        this(SupplyRuleCatalog.builtIn());
    }

    public GstSupplyDecisionEngine(SupplyRuleCatalog catalog) {
        this.catalog = catalog == null ? SupplyRuleCatalog.builtIn() : catalog;
    }

    public SupplyDecision decide(SupplyFacts facts) {
        SupplyFacts input = facts == null ? new SupplyFacts() : facts;
        LocalDate on = input.transactionDate() == null ? LocalDate.now() : input.transactionDate();

        if (input.sameRegistrationTransfer() || is(input, SAME_REGISTRATION_TRANSFER)) {
            return movement(input, on, SAME_REGISTRATION_TRANSFER, "SAME_GSTIN_MOVEMENT",
                    "DELIVERY_CHALLAN", "NONE", "NOT_A_SUPPLY_MOVEMENT",
                    "Movement under the same GST registration is not a supply. Use a delivery challan. No output GST and no ITC reversal.");
        }
        if (Boolean.TRUE.equals(input.distinctRegistration()) || is(input, DISTINCT_REGISTRATION_TRANSFER)) {
            return scheduleOneSupply(input, on, DISTINCT_REGISTRATION_TRANSFER);
        }
        if (Boolean.TRUE.equals(input.relatedParty()) && !hasConsideration(input) && !schemeSale(input)) {
            return scheduleOneSupply(input, on, RELATED_PARTY_SUPPLY);
        }
        if (isCustody(input)) {
            return movement(input, on, MR_STOCK_CUSTODY, "MR_CUSTODY",
                    "DELIVERY_CHALLAN", "STOCK_CUSTODY_ONLY", "NOT_A_SUPPLY_MOVEMENT",
                    "Stock issued to an MR remains stock in custody. It is not a gift, a sale, or an expense until it is distributed or returned.");
        }
        if (isPersonalGift(input)) {
            return personalGift(input, on);
        }
        if (input.invoiceDiscount() || is(input, INVOICE_DISCOUNT)) {
            return invoiceDiscount(input, on);
        }
        if (input.postSaleDiscount() || is(input, POST_SALE_DISCOUNT)) {
            return postSaleDiscount(input, on);
        }
        if (schemeSale(input) || is(input, BUY_X_GET_Y)) {
            return buyXGetY(input, on);
        }
        if (!hasConsideration(input) && relationshipUnknown(input)) {
            return review(input, on, OTHER_PROMOTIONAL_SUPPLY, "CIRC92_A_FREE_SAMPLE",
                    "Relationship or GST registration is not recorded. A zero-consideration issue cannot be treated as an unrelated free sample until that fact is confirmed.");
        }
        if (isPhysician(input)) {
            return freeDisposal(input, on, PHYSICIAN_SAMPLE, "SAMPLE_ISSUE_NOTE",
                    "Free physician sample to an unrelated doctor is not a supply. No output GST. Attributable ITC is blocked under section 17(5)(h).");
        }
        if (isRetailerFree(input)) {
            return freeDisposal(input, on, RETAILER_FREE_PROMOTION, "SAMPLE_ISSUE_NOTE",
                    "Free promotional stock to an unrelated retailer, with no consideration, is not a supply. No output GST. Attributable ITC is blocked under section 17(5)(h).");
        }
        if (is(input, MR_DISTRIBUTION)) {
            return review(input, on, MR_DISTRIBUTION, "CIRC92_A_FREE_SAMPLE",
                    "Final recipient is not a doctor, retailer, or other classified recipient. Authorised review is required before GST treatment.");
        }
        return review(input, on, OTHER_PROMOTIONAL_SUPPLY, "CIRC92_A_FREE_SAMPLE",
                "The movement does not match a configured promotional supply class.");
    }

    private SupplyDecision scheduleOneSupply(SupplyFacts facts, LocalDate on, String classification) {
        SupplyRuleCatalog.SupplyRuleView rule = requireActive(facts, on, classification, "SCHEDULE_I_RELATED_OR_DISTINCT");
        if (rule == null) {
            return review(facts, on, classification, "SCHEDULE_I_RELATED_OR_DISTINCT",
                    "Schedule I rule is not active for this date.");
        }
        Valuation valuation = valueScheduleOne(facts);
        if (valuation == null) {
            return review(facts, on, classification, "SCHEDULE_I_RELATED_OR_DISTINCT",
                    "Rule 28 value is not available. Provide open-market value, like-kind value, cost, or a declared value where the recipient has full ITC.");
        }
        TaxParts tax = taxOn(facts, valuation.amount(), rate(facts));
        if (tax == null) {
            return review(facts, on, classification, "IGST_S10_GOODS",
                    "Supplier or recipient state is missing, so CGST/SGST versus IGST cannot be decided.");
        }
        return determined(facts, on, classification, rule, true, true, valuation.amount(), valuation.method(),
                rate(facts), tax, SupplyDecision.ITC_RETAINED, "TAX_INVOICE", "TRANSFER_TAX_INVOICE", "TAXABLE_OUTWARD",
                "Schedule I treats this transfer as a supply even without consideration. Supplier ITC remains available because the goods are supplied, not gifted.");
    }

    private SupplyDecision personalGift(SupplyFacts facts, LocalDate on) {
        SupplyRuleCatalog.SupplyRuleView rule = requireActive(facts, on, MR_PERSONAL_GIFT, "SCHEDULE_I_EMPLOYEE_GIFT");
        if (rule == null) {
            return review(facts, on, MR_PERSONAL_GIFT, "SCHEDULE_I_EMPLOYEE_GIFT", "Employee gift rule is not active.");
        }
        if (facts.employerEmployee() == null) {
            return review(facts, on, MR_PERSONAL_GIFT, "SCHEDULE_I_EMPLOYEE_GIFT",
                    "Confirm whether the MR is an employee. An independent agent is not covered by the employer-gift proviso.");
        }
        if (!Boolean.TRUE.equals(facts.employerEmployee())) {
            if (relationshipUnknown(facts)) {
                return review(facts, on, MR_PERSONAL_GIFT, "CIRC92_A_FREE_SAMPLE",
                        "Confirm whether this independent MR is a related person or a distinct registration before treating the gift as a free disposal.");
            }
            if (Boolean.TRUE.equals(facts.relatedParty())) {
                return scheduleOneSupply(facts, on, RELATED_PARTY_SUPPLY);
            }
            return freeDisposal(facts, on, MR_PERSONAL_GIFT, "SAMPLE_ISSUE_NOTE",
                    "Gift to a person who is not an employee, with no consideration, is not a supply. ITC is blocked under section 17(5)(h).");
        }
        if (facts.thisGiftCost() == null || facts.thisGiftOmv() == null) {
            return review(facts, on, MR_PERSONAL_GIFT, "SCHEDULE_I_EMPLOYEE_GIFT",
                    "Both cost and open-market value are required. The Rs 50,000 employee-gift cap has no statutory valuation base, so the cap is not applied on one figure alone.");
        }
        BigDecimal costTotal = nz(facts.fyGiftCostToDate()).add(facts.thisGiftCost());
        BigDecimal omvTotal = nz(facts.fyGiftOmvToDate()).add(facts.thisGiftOmv());
        if (costTotal.compareTo(EMPLOYEE_GIFT_CAP) > 0 || omvTotal.compareTo(EMPLOYEE_GIFT_CAP) > 0) {
            return review(facts, on, MR_PERSONAL_GIFT, "SCHEDULE_I_EMPLOYEE_GIFT",
                    "Cumulative gifts may exceed Rs 50,000. Authorised review is required before treating the excess as a Schedule I supply.");
        }
        return freeDisposal(facts, on, MR_PERSONAL_GIFT, "SAMPLE_ISSUE_NOTE",
                "Employee gift is within Rs 50,000 on both cost and open-market value, so the Schedule I proviso applies. It is not a supply. ITC is blocked under section 17(5)(h).");
    }

    private SupplyDecision buyXGetY(SupplyFacts facts, LocalDate on) {
        if (!positive(facts.paidQuantity()) || !positive(facts.freeQuantity()) || !positive(facts.chargedAmount())) {
            return review(facts, on, BUY_X_GET_Y, "CIRC92_B_BUY_X_GET_Y",
                    "Buy X get Y needs a paid quantity, a free quantity, and one charged price for the combined supply.");
        }
        boolean mixed = higherRateFreeProduct(facts);
        String ruleCode = mixed ? "SEC_8_MIXED" : "CIRC92_B_BUY_X_GET_Y";
        SupplyRuleCatalog.SupplyRuleView rule = requireActive(facts, on, BUY_X_GET_Y, ruleCode);
        if (rule == null) {
            return review(facts, on, BUY_X_GET_Y, ruleCode, "Promotional sale rule is not active for this date.");
        }
        double appliedRate = mixed ? facts.freeProductGstPercent() : rate(facts);
        TaxParts tax = taxOn(facts, facts.chargedAmount(), appliedRate);
        if (tax == null) {
            return review(facts, on, BUY_X_GET_Y, "IGST_S10_GOODS",
                    "Supplier or recipient state is missing, so CGST/SGST versus IGST cannot be decided.");
        }
        String explanation = mixed
                ? "The free product has a different rate. Section 8 treats the bundle as a mixed supply taxed at the highest rate on the single price charged. The extra quantity is not an independent gift. Supplier ITC stays available."
                : "Buy X get Y is two quantities for one price. Tax is charged on the amount received. The extra quantity is not an independent gift. Supplier ITC stays available.";
        return determined(facts, on, BUY_X_GET_Y, rule, true, appliedRate > 0, facts.chargedAmount(), "TRANSACTION_VALUE",
                appliedRate, tax, SupplyDecision.ITC_RETAINED, "TAX_INVOICE", "SALES_INVOICE", "TAXABLE_OUTWARD", explanation);
    }

    private SupplyDecision invoiceDiscount(SupplyFacts facts, LocalDate on) {
        SupplyRuleCatalog.SupplyRuleView rule = requireActive(facts, on, INVOICE_DISCOUNT, "SEC_15_3_A_INVOICE_DISCOUNT");
        if (rule == null) {
            return review(facts, on, INVOICE_DISCOUNT, "SEC_15_3_A_INVOICE_DISCOUNT", "Invoice discount rule is not active.");
        }
        BigDecimal net = positive(facts.chargedAmount()) ? facts.chargedAmount() : BigDecimal.ZERO;
        TaxParts tax = taxOn(facts, net, rate(facts));
        if (tax == null) {
            return review(facts, on, INVOICE_DISCOUNT, "IGST_S10_GOODS",
                    "Supplier or recipient state is missing, so CGST/SGST versus IGST cannot be decided.");
        }
        return determined(facts, on, INVOICE_DISCOUNT, rule, true, rate(facts) > 0, net, "TRANSACTION_VALUE_AFTER_INVOICE_DISCOUNT",
                rate(facts), tax, SupplyDecision.ITC_RETAINED, "TAX_INVOICE", "DISCOUNT_ON_INVOICE", "TAXABLE_OUTWARD",
                "Discount recorded on the original invoice is excluded from taxable value under section 15(3)(a). Supplier ITC stays available.");
    }

    private SupplyDecision postSaleDiscount(SupplyFacts facts, LocalDate on) {
        if (facts.commercialCreditNote()) {
            return commercialCredit(facts, on);
        }
        if (!on.isBefore(POST_SALE_AMENDMENT)) {
            SupplyRuleCatalog.SupplyRuleView amended = catalog.find("SEC_15_3_B_POST_SALE_FROM_2025_10_01", on);
            if (amended == null || !amended.activated() || SupplyDecision.REVIEW.equals(amended.outcomeStatus())) {
                return review(facts, on, POST_SALE_DISCOUNT, "SEC_15_3_B_POST_SALE_FROM_2025_10_01",
                        "From 1 October 2025 the post-supply discount amendment is not confirmed in this configuration. Issue a commercial credit note, or ask an authorised GST user to activate the confirmed statutory text before reducing taxable value.");
            }
            if (!facts.linkedToInvoice()) {
                return commercialCredit(facts, on);
            }
            BigDecimal amendedDiscount = nz(facts.discountAmount());
            TaxParts amendedTax = taxOn(facts, amendedDiscount, rate(facts));
            if (amendedTax == null) {
                return review(facts, on, POST_SALE_DISCOUNT, "IGST_S10_GOODS",
                        "Supplier or recipient state is missing, so the GST credit note split cannot be decided.");
            }
            return determined(facts, on, POST_SALE_DISCOUNT, amended, true, rate(facts) > 0, amendedDiscount,
                    "SECTION_34_CREDIT_NOTE", rate(facts), amendedTax, SupplyDecision.ITC_RETAINED,
                    "GST_CREDIT_NOTE", "GST_CREDIT_NOTE", "VALUE_ADJUSTMENT",
                    "Authorised configuration is active for post-supply discounts from 1 October 2025. Taxable value is reduced only because the discount is linked to a credit note. This is not a GSTR-1 filing.");
        }
        SupplyRuleCatalog.SupplyRuleView rule = requireActive(facts, on, POST_SALE_DISCOUNT, "SEC_15_3_B_POST_SALE_UNTIL_2025_09_30");
        if (rule == null) {
            return review(facts, on, POST_SALE_DISCOUNT, "SEC_15_3_B_POST_SALE_UNTIL_2025_09_30",
                    "Post-supply discount rule is not active for this date.");
        }
        if (!(facts.priorAgreement() && facts.linkedToInvoice() && facts.recipientItcReversed())) {
            return commercialCredit(facts, on);
        }
        BigDecimal discount = nz(facts.discountAmount());
        TaxParts tax = taxOn(facts, discount, rate(facts));
        if (tax == null) {
            return review(facts, on, POST_SALE_DISCOUNT, "IGST_S10_GOODS",
                    "Supplier or recipient state is missing, so the GST credit note split cannot be decided.");
        }
        return determined(facts, on, POST_SALE_DISCOUNT, rule, true, rate(facts) > 0, discount, "SECTION_15_3_B",
                rate(facts), tax, SupplyDecision.ITC_RETAINED, "GST_CREDIT_NOTE", "GST_CREDIT_NOTE", "VALUE_ADJUSTMENT",
                "Post-supply discount meets section 15(3)(b) as in force before 1 October 2025. Taxable value is reduced. Supplier ITC stays available. This books classification is not a GSTR-1 filing.");
    }

    private SupplyDecision commercialCredit(SupplyFacts facts, LocalDate on) {
        SupplyRuleCatalog.SupplyRuleView rule = catalog.find("SEC_15_3_A_INVOICE_DISCOUNT", on);
        String legal = rule == null ? "CGST Act s.15(3) and s.34" : rule.legalReference();
        return new SupplyDecision(
                POST_SALE_DISCOUNT,
                "SEC_15_3_B_POST_SALE_UNTIL_2025_09_30",
                legal,
                LocalDate.of(2017, 7, 1),
                SupplyDecision.DETERMINED,
                false,
                false,
                BigDecimal.ZERO.setScale(2),
                "COMMERCIAL_NO_GST_REDUCTION",
                rate(facts),
                false,
                zero(), zero(), zero(), zero(),
                SupplyDecision.ITC_RETAINED,
                zero(), zero(), zero(), zero(),
                "COMMERCIAL_CREDIT_NOTE",
                "COMMERCIAL_CREDIT_NOTE",
                "EXCLUDED_NOT_A_SUPPLY",
                false,
                "Conditions for a taxable-value reduction are not met. Issue a commercial credit note. Do not reduce output GST.");
    }

    private SupplyDecision freeDisposal(SupplyFacts facts, LocalDate on, String classification, String document, String explanation) {
        SupplyRuleCatalog.SupplyRuleView rule = requireActive(facts, on, classification, "CIRC92_A_FREE_SAMPLE");
        if (rule == null) {
            return review(facts, on, classification, "CIRC92_A_FREE_SAMPLE", "Free sample rule is not active for this date.");
        }
        String itc = facts.purchaseTaxTraced() ? SupplyDecision.ITC_REVERSAL : SupplyDecision.ITC_PENDING;
        BigDecimal cgst = facts.purchaseTaxTraced() ? nz(facts.attributableCgst()) : zero();
        BigDecimal sgst = facts.purchaseTaxTraced() ? nz(facts.attributableSgst()) : zero();
        BigDecimal igst = facts.purchaseTaxTraced() ? nz(facts.attributableIgst()) : zero();
        BigDecimal cess = facts.purchaseTaxTraced() ? nz(facts.attributableCess()) : zero();
        String itcText = facts.purchaseTaxTraced()
                ? " Attributable purchase tax is ready for reversal."
                : " Purchase tax is not traced to this batch, so ITC reversal stays pending review and is not posted.";
        return new SupplyDecision(
                classification,
                rule.ruleCode(),
                rule.legalReference() + " " + itcLegal(),
                rule.effectiveFrom(),
                SupplyDecision.DETERMINED,
                false,
                false,
                BigDecimal.ZERO.setScale(2),
                "NO_CONSIDERATION",
                rate(facts),
                false,
                zero(), zero(), zero(), zero(),
                itc,
                cgst, sgst, igst, cess,
                document,
                "DR_PROMOTION_CR_STOCK_AND_INPUT_GST",
                "EXCLUDED_NOT_A_SUPPLY",
                false,
                explanation + itcText + " Books exclusion is not a GSTR-1 or GSTR-3B portal filing.");
    }

    private SupplyDecision movement(
            SupplyFacts facts,
            LocalDate on,
            String classification,
            String ruleCode,
            String document,
            String accounting,
            String books,
            String explanation) {
        SupplyRuleCatalog.SupplyRuleView rule = requireActive(facts, on, classification, ruleCode);
        if (rule == null) {
            return review(facts, on, classification, ruleCode, "Movement rule is not active for this date.");
        }
        return determined(facts, on, classification, rule, false, false, zero(), "NOT_A_SUPPLY",
                0, TaxParts.none(), SupplyDecision.ITC_NA, document, accounting, books, explanation);
    }

    private SupplyDecision review(SupplyFacts facts, LocalDate on, String classification, String ruleCode, String explanation) {
        SupplyRuleCatalog.SupplyRuleView rule = catalog.find(ruleCode, on);
        String legal = rule == null ? ruleCode : rule.legalReference();
        LocalDate from = rule == null ? LocalDate.of(2017, 7, 1) : rule.effectiveFrom();
        return new SupplyDecision(
                classification,
                ruleCode,
                legal,
                from,
                SupplyDecision.REVIEW,
                false,
                false,
                zero(),
                "UNRESOLVED",
                rate(facts),
                false,
                zero(), zero(), zero(), zero(),
                SupplyDecision.ITC_PENDING,
                zero(), zero(), zero(), zero(),
                "NONE",
                "NONE",
                "EXCLUDED_NOT_A_SUPPLY",
                false,
                explanation);
    }

    private SupplyRuleCatalog.SupplyRuleView requireActive(SupplyFacts facts, LocalDate on, String classification, String ruleCode) {
        SupplyRuleCatalog.SupplyRuleView rule = catalog.find(ruleCode, on);
        if (rule == null || !rule.activated() || SupplyDecision.REVIEW.equals(rule.outcomeStatus())) {
            return null;
        }
        return rule;
    }

    private SupplyDecision determined(
            SupplyFacts facts,
            LocalDate on,
            String classification,
            SupplyRuleCatalog.SupplyRuleView rule,
            boolean supply,
            boolean outputGst,
            BigDecimal taxable,
            String valuation,
            double gstPercent,
            TaxParts tax,
            String itc,
            String document,
            String accounting,
            String books,
            String explanation) {
        return new SupplyDecision(
                classification,
                rule.ruleCode(),
                rule.legalReference(),
                rule.effectiveFrom(),
                SupplyDecision.DETERMINED,
                supply,
                outputGst,
                scale(taxable),
                valuation,
                gstPercent,
                tax.interState(),
                tax.cgst(),
                tax.sgst(),
                tax.igst(),
                tax.cess(),
                itc,
                zero(), zero(), zero(), zero(),
                document,
                accounting,
                books,
                false,
                explanation + " This books classification is not a GSTR-1 or GSTR-3B portal filing.");
    }

    private Valuation valueScheduleOne(SupplyFacts facts) {
        if (Boolean.TRUE.equals(facts.recipientFullItc()) && facts.declaredInvoiceValue() != null) {
            return new Valuation(scale(facts.declaredInvoiceValue()), "RULE_28_INVOICE_DEEMED_OMV");
        }
        if (facts.openMarketValue() != null) {
            return new Valuation(scale(facts.openMarketValue()), "RULE_28_OPEN_MARKET");
        }
        if (facts.likeKindValue() != null) {
            return new Valuation(scale(facts.likeKindValue()), "RULE_28_LIKE_KIND");
        }
        if (facts.cost() != null) {
            BigDecimal costPlus = facts.cost().multiply(new BigDecimal("1.10"));
            return new Valuation(scale(costPlus), "RULE_30_COST_PLUS_10");
        }
        return null;
    }

    private TaxParts taxOn(SupplyFacts facts, BigDecimal taxable, double rate) {
        Boolean inter = interState(facts);
        if (inter == null) {
            return null;
        }
        BigDecimal base = scale(taxable);
        BigDecimal tax = GstMoney.percentOf(base, rate);
        BigDecimal cess = GstMoney.percentOf(base, facts.cessPercent() == null ? 0 : facts.cessPercent());
        GstTaxSplit split = GstTaxSplit.split(tax, cess, inter);
        return new TaxParts(inter, split.cgst(), split.sgst(), split.igst(), split.cess());
    }

    private static Boolean interState(SupplyFacts facts) {
        if (blank(facts.sellerStateCode()) || blank(facts.recipientStateCode())) {
            return null;
        }
        return !facts.sellerStateCode().trim().equals(facts.recipientStateCode().trim());
    }

    private static boolean higherRateFreeProduct(SupplyFacts facts) {
        if (facts.compositeSupply() || facts.freeProductGstPercent() == null) {
            return false;
        }
        return facts.freeProductGstPercent() > rate(facts);
    }

    private static boolean isCustody(SupplyFacts facts) {
        if (is(facts, MR_DISTRIBUTION) || is(facts, MR_PERSONAL_GIFT)) {
            return false;
        }
        return is(facts, MR_STOCK_CUSTODY) || eq(facts.issueType(), "FIELD_STOCK");
    }

    private static boolean isPersonalGift(SupplyFacts facts) {
        return is(facts, MR_PERSONAL_GIFT)
                || (eq(facts.recipientType(), "EMPLOYEE") && (eq(facts.issueType(), "PROMOTIONAL") || eq(facts.issueType(), "SAMPLE")));
    }

    private static boolean isPhysician(SupplyFacts facts) {
        if (hasConsideration(facts) || !Boolean.FALSE.equals(facts.relatedParty())) {
            return false;
        }
        return is(facts, PHYSICIAN_SAMPLE)
                || is(facts, MR_DISTRIBUTION) && eq(facts.recipientType(), "DOCTOR")
                || eq(facts.issueType(), "SAMPLE") && eq(facts.recipientType(), "DOCTOR");
    }

    private static boolean isRetailerFree(SupplyFacts facts) {
        if (hasConsideration(facts) || !Boolean.FALSE.equals(facts.relatedParty())) {
            return false;
        }
        return is(facts, RETAILER_FREE_PROMOTION)
                || is(facts, MR_DISTRIBUTION) && (eq(facts.recipientType(), "PHARMACY") || eq(facts.recipientType(), "CUSTOMER"))
                || (eq(facts.issueType(), "PROMOTIONAL") || eq(facts.issueType(), "FREE_ISSUE") || eq(facts.issueType(), "SAMPLE"))
                        && (eq(facts.recipientType(), "PHARMACY") || eq(facts.recipientType(), "CUSTOMER"));
    }

    private static boolean schemeSale(SupplyFacts facts) {
        return is(facts, BUY_X_GET_Y)
                || positive(facts.paidQuantity()) && positive(facts.freeQuantity()) && positive(facts.chargedAmount());
    }

    private static boolean hasConsideration(SupplyFacts facts) {
        return positive(facts.consideration()) || positive(facts.chargedAmount());
    }

    private static boolean relationshipUnknown(SupplyFacts facts) {
        return facts.relatedParty() == null || facts.distinctRegistration() == null;
    }

    private static boolean is(SupplyFacts facts, String code) {
        return eq(facts.requestedClassification(), code);
    }

    private static boolean eq(String value, String expected) {
        return value != null && expected.equalsIgnoreCase(value.trim());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? zero() : scale(value);
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, GstMoney.ROUND);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static double rate(SupplyFacts facts) {
        return facts.gstPercent() == null ? 0 : facts.gstPercent();
    }

    private static String itcLegal() {
        return "ITC: CGST Act s.17(5)(h).";
    }

    private record Valuation(BigDecimal amount, String method) {
    }

    private record TaxParts(boolean interState, BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal cess) {
        static TaxParts none() {
            return new TaxParts(false, zero(), zero(), zero(), zero());
        }
    }
}
