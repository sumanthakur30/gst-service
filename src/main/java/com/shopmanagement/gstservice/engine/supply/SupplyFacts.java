package com.shopmanagement.gstservice.engine.supply;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Facts the decision engine is allowed to see. Callers must not send a chosen tax treatment.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class SupplyFacts {

    private LocalDate transactionDate = LocalDate.now();
    private String requestedClassification;
    private String issueType;
    private String recipientType;
    private BigDecimal consideration;
    private BigDecimal chargedAmount;
    private BigDecimal paidQuantity;
    private BigDecimal freeQuantity;
    private BigDecimal discountAmount;
    private Boolean relatedParty;
    private Boolean distinctRegistration;
    private boolean sameRegistrationTransfer;
    private Boolean employerEmployee;
    private Double gstPercent;
    private Double freeProductGstPercent;
    private Double cessPercent;
    private String hsn;
    private String freeProductHsn;
    private boolean compositeSupply;
    private String sellerStateCode;
    private String recipientStateCode;
    private BigDecimal openMarketValue;
    private BigDecimal likeKindValue;
    private BigDecimal cost;
    private Boolean recipientFullItc;
    private BigDecimal declaredInvoiceValue;
    private boolean invoiceDiscount;
    private boolean postSaleDiscount;
    private boolean priorAgreement;
    private boolean linkedToInvoice;
    private boolean recipientItcReversed;
    private boolean commercialCreditNote;
    private BigDecimal fyGiftCostToDate;
    private BigDecimal fyGiftOmvToDate;
    private BigDecimal thisGiftCost;
    private BigDecimal thisGiftOmv;
    private boolean purchaseTaxTraced;
    private BigDecimal attributableCgst = BigDecimal.ZERO;
    private BigDecimal attributableSgst = BigDecimal.ZERO;
    private BigDecimal attributableIgst = BigDecimal.ZERO;
    private BigDecimal attributableCess = BigDecimal.ZERO;

    public LocalDate transactionDate() { return transactionDate; }
    public SupplyFacts transactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; return this; }
    public String requestedClassification() { return requestedClassification; }
    public SupplyFacts requestedClassification(String requestedClassification) { this.requestedClassification = requestedClassification; return this; }
    public String issueType() { return issueType; }
    public SupplyFacts issueType(String issueType) { this.issueType = issueType; return this; }
    public String recipientType() { return recipientType; }
    public SupplyFacts recipientType(String recipientType) { this.recipientType = recipientType; return this; }
    public BigDecimal consideration() { return consideration; }
    public SupplyFacts consideration(BigDecimal consideration) { this.consideration = consideration; return this; }
    public BigDecimal chargedAmount() { return chargedAmount; }
    public SupplyFacts chargedAmount(BigDecimal chargedAmount) { this.chargedAmount = chargedAmount; return this; }
    public BigDecimal paidQuantity() { return paidQuantity; }
    public SupplyFacts paidQuantity(BigDecimal paidQuantity) { this.paidQuantity = paidQuantity; return this; }
    public BigDecimal freeQuantity() { return freeQuantity; }
    public SupplyFacts freeQuantity(BigDecimal freeQuantity) { this.freeQuantity = freeQuantity; return this; }
    public BigDecimal discountAmount() { return discountAmount; }
    public SupplyFacts discountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
    public Boolean relatedParty() { return relatedParty; }
    public SupplyFacts relatedParty(Boolean relatedParty) { this.relatedParty = relatedParty; return this; }
    public Boolean distinctRegistration() { return distinctRegistration; }
    public SupplyFacts distinctRegistration(Boolean distinctRegistration) { this.distinctRegistration = distinctRegistration; return this; }
    public boolean sameRegistrationTransfer() { return sameRegistrationTransfer; }
    public SupplyFacts sameRegistrationTransfer(boolean sameRegistrationTransfer) { this.sameRegistrationTransfer = sameRegistrationTransfer; return this; }
    public Boolean employerEmployee() { return employerEmployee; }
    public SupplyFacts employerEmployee(Boolean employerEmployee) { this.employerEmployee = employerEmployee; return this; }
    public Double gstPercent() { return gstPercent; }
    public SupplyFacts gstPercent(Double gstPercent) { this.gstPercent = gstPercent; return this; }
    public Double freeProductGstPercent() { return freeProductGstPercent; }
    public SupplyFacts freeProductGstPercent(Double freeProductGstPercent) { this.freeProductGstPercent = freeProductGstPercent; return this; }
    public Double cessPercent() { return cessPercent; }
    public SupplyFacts cessPercent(Double cessPercent) { this.cessPercent = cessPercent; return this; }
    public String hsn() { return hsn; }
    public SupplyFacts hsn(String hsn) { this.hsn = hsn; return this; }
    public String freeProductHsn() { return freeProductHsn; }
    public SupplyFacts freeProductHsn(String freeProductHsn) { this.freeProductHsn = freeProductHsn; return this; }
    public boolean compositeSupply() { return compositeSupply; }
    public SupplyFacts compositeSupply(boolean compositeSupply) { this.compositeSupply = compositeSupply; return this; }
    public String sellerStateCode() { return sellerStateCode; }
    public SupplyFacts sellerStateCode(String sellerStateCode) { this.sellerStateCode = sellerStateCode; return this; }
    public String recipientStateCode() { return recipientStateCode; }
    public SupplyFacts recipientStateCode(String recipientStateCode) { this.recipientStateCode = recipientStateCode; return this; }
    public BigDecimal openMarketValue() { return openMarketValue; }
    public SupplyFacts openMarketValue(BigDecimal openMarketValue) { this.openMarketValue = openMarketValue; return this; }
    public BigDecimal likeKindValue() { return likeKindValue; }
    public SupplyFacts likeKindValue(BigDecimal likeKindValue) { this.likeKindValue = likeKindValue; return this; }
    public BigDecimal cost() { return cost; }
    public SupplyFacts cost(BigDecimal cost) { this.cost = cost; return this; }
    public Boolean recipientFullItc() { return recipientFullItc; }
    public SupplyFacts recipientFullItc(Boolean recipientFullItc) { this.recipientFullItc = recipientFullItc; return this; }
    public BigDecimal declaredInvoiceValue() { return declaredInvoiceValue; }
    public SupplyFacts declaredInvoiceValue(BigDecimal declaredInvoiceValue) { this.declaredInvoiceValue = declaredInvoiceValue; return this; }
    public boolean invoiceDiscount() { return invoiceDiscount; }
    public SupplyFacts invoiceDiscount(boolean invoiceDiscount) { this.invoiceDiscount = invoiceDiscount; return this; }
    public boolean postSaleDiscount() { return postSaleDiscount; }
    public SupplyFacts postSaleDiscount(boolean postSaleDiscount) { this.postSaleDiscount = postSaleDiscount; return this; }
    public boolean priorAgreement() { return priorAgreement; }
    public SupplyFacts priorAgreement(boolean priorAgreement) { this.priorAgreement = priorAgreement; return this; }
    public boolean linkedToInvoice() { return linkedToInvoice; }
    public SupplyFacts linkedToInvoice(boolean linkedToInvoice) { this.linkedToInvoice = linkedToInvoice; return this; }
    public boolean recipientItcReversed() { return recipientItcReversed; }
    public SupplyFacts recipientItcReversed(boolean recipientItcReversed) { this.recipientItcReversed = recipientItcReversed; return this; }
    public boolean commercialCreditNote() { return commercialCreditNote; }
    public SupplyFacts commercialCreditNote(boolean commercialCreditNote) { this.commercialCreditNote = commercialCreditNote; return this; }
    public BigDecimal fyGiftCostToDate() { return fyGiftCostToDate; }
    public SupplyFacts fyGiftCostToDate(BigDecimal fyGiftCostToDate) { this.fyGiftCostToDate = fyGiftCostToDate; return this; }
    public BigDecimal fyGiftOmvToDate() { return fyGiftOmvToDate; }
    public SupplyFacts fyGiftOmvToDate(BigDecimal fyGiftOmvToDate) { this.fyGiftOmvToDate = fyGiftOmvToDate; return this; }
    public BigDecimal thisGiftCost() { return thisGiftCost; }
    public SupplyFacts thisGiftCost(BigDecimal thisGiftCost) { this.thisGiftCost = thisGiftCost; return this; }
    public BigDecimal thisGiftOmv() { return thisGiftOmv; }
    public SupplyFacts thisGiftOmv(BigDecimal thisGiftOmv) { this.thisGiftOmv = thisGiftOmv; return this; }
    public boolean purchaseTaxTraced() { return purchaseTaxTraced; }
    public SupplyFacts purchaseTaxTraced(boolean purchaseTaxTraced) { this.purchaseTaxTraced = purchaseTaxTraced; return this; }
    public BigDecimal attributableCgst() { return attributableCgst; }
    public SupplyFacts attributableCgst(BigDecimal attributableCgst) { this.attributableCgst = attributableCgst; return this; }
    public BigDecimal attributableSgst() { return attributableSgst; }
    public SupplyFacts attributableSgst(BigDecimal attributableSgst) { this.attributableSgst = attributableSgst; return this; }
    public BigDecimal attributableIgst() { return attributableIgst; }
    public SupplyFacts attributableIgst(BigDecimal attributableIgst) { this.attributableIgst = attributableIgst; return this; }
    public BigDecimal attributableCess() { return attributableCess; }
    public SupplyFacts attributableCess(BigDecimal attributableCess) { this.attributableCess = attributableCess; return this; }
}
