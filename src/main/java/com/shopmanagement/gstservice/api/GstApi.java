package com.shopmanagement.gstservice.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.shopmanagement.gstservice.model.DocumentType;
import com.shopmanagement.gstservice.model.RegistrationType;
import com.shopmanagement.gstservice.model.SupplyType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class GstApi {

    private GstApi() {
    }

    public record GstRegistrationResponse(
            Long id,
            String legalName,
            String gstin,
            String stateCode,
            RegistrationType registrationType,
            boolean active,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {
    }

    public record GstRegistrationUpsert(
            @NotBlank @Size(max = 300) String legalName,
            @NotBlank @Size(min = 15, max = 15) String gstin,
            @NotBlank @Size(min = 2, max = 2) String stateCode,
            RegistrationType registrationType,
            Boolean active,
            LocalDate effectiveFrom) {
    }

    public record HsnSacResponse(Long id, String code, String description, String type, String chapter) {
    }

    public record GstinValidateRequest(@NotBlank String gstin) {
    }

    public record GstinValidateResponse(
            boolean validFormat,
            String normalizedGstin,
            String stateCode,
            String message) {
    }

    public record TaxLineRequest(
            int lineNo,
            Long productId,
            String hsnSac,
            @NotNull Double quantity,
            @NotNull Double unitPrice,
            Double gstPercent,
            Double discountAmount,
            Double schemeDiscount,
            Double freeQuantity,
            Double cessPercent,
            Double mrp,
            String batchRef,
            String taxCategoryCode,
            Boolean taxInclusive,
            Boolean reverseCharge) {
    }

    public record TaxCalculateRequest(
            boolean applyGst,
            String sellerStateCode,
            String customerStateCode,
            String sellerGstin,
            String customerGstin,
            Double headerDiscount,
            Double incomingTaxAmount,
            List<@Valid TaxLineRequest> lines,
            String businessType,
            String customerGstType,
            String pricingMode,
            LocalDate transactionDate,
            Double invoiceLevelDiscount,
            Boolean discountBeforeTax,
            Boolean reverseCharge,
            Map<String, String> businessAttributes) {
    }

    public record TaxLineResult(
            int lineNo,
            Double gstPercentApplied,
            double taxableValue,
            double taxAmount,
            double lineTotal,
            boolean taxInclusive,
            Double cessAmount,
            String supplyNature,
            String ruleApplied,
            String hsnSac) {
    }

    public record TaxCalculateResponse(
            boolean gstEnabled,
            boolean interState,
            double subtotalAmount,
            double discountAmount,
            double taxableAmount,
            double taxAmount,
            double cgstAmount,
            double sgstAmount,
            double igstAmount,
            double totalAmount,
            List<TaxLineResult> lines,
            Double cessAmount,
            Double roundOff,
            String businessType,
            List<String> validationWarnings) {
    }

    public record TaxValidationRequest(
            String gstin,
            String stateCode,
            String hsnSac,
            String sellerStateCode,
            String customerStateCode,
            String customerGstType) {
    }

    public record TaxValidationResponse(
            boolean valid,
            List<String> errors,
            List<String> warnings) {
    }

    public record GstSlabResponse(String slabCode, double gstRatePercent, double cessPercent, String supplyNature) {
    }

    public record GstrSummaryRequest(
            @NotNull LocalDate fromDate,
            @NotNull LocalDate toDate,
            Long gstRegistrationId) {
    }

    public record GstrSummaryResponse(
            LocalDate fromDate,
            LocalDate toDate,
            double taxableValue,
            double cgst,
            double sgst,
            double igst,
            double cess,
            int documentCount,
            String gstr1Json,
            String gstr3bJson,
            String hsnSummaryJson) {
    }

    public record TaxReverseRequest(
            @NotNull Long snapshotId,
            String reason) {
    }

    public record BranchMapResponse(
            Long id,
            Long gstRegistrationId,
            String gstin,
            Long branchId,
            String shopId,
            boolean isDefault) {
    }

    public record BranchMapUpsert(
            @NotNull Long gstRegistrationId,
            Long branchId,
            @NotBlank @Size(max = 64) String shopId,
            Boolean isDefault) {
    }

    public record InvoiceNumberRequest(
            @NotNull Long gstRegistrationId,
            @NotNull LocalDate documentDate,
            @Size(max = 20) String seriesPrefix) {
    }

    public record InvoiceNumberResponse(
            String financialYear,
            String invoiceNumber,
            long sequence) {
    }

    public record TaxDocumentPostRequest(
            @NotBlank @Size(max = 40) String sourceService,
            @NotBlank @Size(max = 40) String sourceType,
            @NotBlank @Size(max = 64) String sourceId,
            @Size(max = 64) String sourceNumber,
            @NotNull DocumentType documentType,
            @NotNull LocalDate documentDate,
            @Size(max = 64) String shopId,
            Long sellerGstRegistrationId,
            String buyerGstin,
            String buyerStateCode,
            String placeOfSupplyState,
            SupplyType supplyType,
            Boolean assignInvoiceNumber,
            @Size(max = 20) String invoiceSeriesPrefix,
            @Valid @NotNull TaxCalculateRequest tax) {
    }

    public record TaxDocumentSnapshotResponse(
            Long id,
            String sourceService,
            String sourceType,
            String sourceId,
            String sourceNumber,
            DocumentType documentType,
            LocalDate documentDate,
            Long sellerGstRegistrationId,
            String buyerGstin,
            SupplyType supplyType,
            double subtotal,
            double discount,
            double taxableValue,
            double totalTax,
            double cgst,
            double sgst,
            double igst,
            double grandTotal,
            String snapshotHash,
            Long originalSnapshotId,
            List<TaxLineSnapshotResponse> lines) {
    }

    public record TaxLineSnapshotResponse(
            int lineNo,
            Long productId,
            String description,
            String hsnSac,
            double quantity,
            double unitPrice,
            double discount,
            double taxableValue,
            Double gstRatePercent,
            double cgst,
            double sgst,
            double igst,
            double lineTotal,
            boolean taxInclusive) {
    }

    public record CreditNotePostRequest(
            @NotNull Long originalSnapshotId,
            @NotBlank @Size(max = 40) String sourceService,
            @NotBlank @Size(max = 40) String sourceType,
            @NotBlank @Size(max = 64) String sourceId,
            @Size(max = 64) String sourceNumber,
            @NotNull LocalDate documentDate,
            @NotNull Double returnAmount,
            String reason) {
    }
}
