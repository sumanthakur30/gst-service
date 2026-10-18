package com.shopmanagement.gstservice.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.engine.supply.SupplyDecision;
import com.shopmanagement.gstservice.engine.supply.SupplyFacts;
import com.shopmanagement.gstservice.model.GstSupplyDecisionRecord;
import com.shopmanagement.gstservice.service.GstSupplyDecisionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/gst/supply-decisions")
public class GstSupplyDecisionController {

    private final GstSupplyDecisionService service;

    public GstSupplyDecisionController(GstSupplyDecisionService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public SupplyDecision preview(@RequestBody SupplyFactsRequest request) {
        return service.preview(request.toFacts());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GstSupplyDecisionRecord record(
            @RequestHeader("X-Shop-Id") String shopId,
            @Valid @RequestBody RecordRequest request) {
        return service.record(shopId, request.sourceType(), request.sourceId(), request.facts().toFacts(), request.recalculate());
    }

    @GetMapping("/{id}")
    public GstSupplyDecisionRecord get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/rules/post-sale-from-2025-10-01/activate")
    public void activatePostSaleRule(
            @RequestHeader(value = "X-Auth-Permissions", required = false) String permissions,
            @RequestHeader(value = "X-Auth-Role", required = false) String role,
            @RequestHeader(value = "X-Auth-User", required = false) String user,
            @RequestBody ActivateRequest request) {
        if (!canManageGst(permissions, role)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "MANAGE_GST is required");
        }
        service.activatePostSaleRule(user == null ? "unknown" : user, request.reason());
    }

    private static boolean canManageGst(String permissions, String role) {
        if (role != null && (role.equalsIgnoreCase("SUPER_ADMIN") || role.equalsIgnoreCase("SHOP_OWNER"))) {
            return true;
        }
        return permissions != null && permissions.toUpperCase().contains("MANAGE_GST");
    }

    public record SupplyFactsRequest(
            LocalDate transactionDate,
            String requestedClassification,
            String issueType,
            String recipientType,
            BigDecimal consideration,
            BigDecimal chargedAmount,
            BigDecimal paidQuantity,
            BigDecimal freeQuantity,
            BigDecimal discountAmount,
            Boolean relatedParty,
            Boolean distinctRegistration,
            Boolean sameRegistrationTransfer,
            Boolean employerEmployee,
            Double gstPercent,
            Double freeProductGstPercent,
            Double cessPercent,
            String hsn,
            String freeProductHsn,
            Boolean compositeSupply,
            String sellerStateCode,
            String recipientStateCode,
            BigDecimal openMarketValue,
            BigDecimal likeKindValue,
            BigDecimal cost,
            Boolean recipientFullItc,
            BigDecimal declaredInvoiceValue,
            Boolean invoiceDiscount,
            Boolean postSaleDiscount,
            Boolean priorAgreement,
            Boolean linkedToInvoice,
            Boolean recipientItcReversed,
            Boolean commercialCreditNote,
            BigDecimal fyGiftCostToDate,
            BigDecimal fyGiftOmvToDate,
            BigDecimal thisGiftCost,
            BigDecimal thisGiftOmv,
            Boolean purchaseTaxTraced,
            BigDecimal attributableCgst,
            BigDecimal attributableSgst,
            BigDecimal attributableIgst,
            BigDecimal attributableCess) {

        SupplyFacts toFacts() {
            SupplyFacts facts = new SupplyFacts();
            return facts.transactionDate(transactionDate == null ? LocalDate.now() : transactionDate)
                    .requestedClassification(requestedClassification)
                    .issueType(issueType)
                    .recipientType(recipientType)
                    .consideration(consideration)
                    .chargedAmount(chargedAmount)
                    .paidQuantity(paidQuantity)
                    .freeQuantity(freeQuantity)
                    .discountAmount(discountAmount)
                    .relatedParty(relatedParty)
                    .distinctRegistration(distinctRegistration)
                    .sameRegistrationTransfer(Boolean.TRUE.equals(sameRegistrationTransfer))
                    .employerEmployee(employerEmployee)
                    .gstPercent(gstPercent)
                    .freeProductGstPercent(freeProductGstPercent)
                    .cessPercent(cessPercent)
                    .hsn(hsn)
                    .freeProductHsn(freeProductHsn)
                    .compositeSupply(Boolean.TRUE.equals(compositeSupply))
                    .sellerStateCode(sellerStateCode)
                    .recipientStateCode(recipientStateCode)
                    .openMarketValue(openMarketValue)
                    .likeKindValue(likeKindValue)
                    .cost(cost)
                    .recipientFullItc(recipientFullItc)
                    .declaredInvoiceValue(declaredInvoiceValue)
                    .invoiceDiscount(Boolean.TRUE.equals(invoiceDiscount))
                    .postSaleDiscount(Boolean.TRUE.equals(postSaleDiscount))
                    .priorAgreement(Boolean.TRUE.equals(priorAgreement))
                    .linkedToInvoice(Boolean.TRUE.equals(linkedToInvoice))
                    .recipientItcReversed(Boolean.TRUE.equals(recipientItcReversed))
                    .commercialCreditNote(Boolean.TRUE.equals(commercialCreditNote))
                    .fyGiftCostToDate(fyGiftCostToDate)
                    .fyGiftOmvToDate(fyGiftOmvToDate)
                    .thisGiftCost(thisGiftCost)
                    .thisGiftOmv(thisGiftOmv)
                    .purchaseTaxTraced(Boolean.TRUE.equals(purchaseTaxTraced))
                    .attributableCgst(attributableCgst)
                    .attributableSgst(attributableSgst)
                    .attributableIgst(attributableIgst)
                    .attributableCess(attributableCess);
        }
    }

    public record RecordRequest(
            @NotBlank String sourceType,
            @NotBlank String sourceId,
            boolean recalculate,
            @Valid SupplyFactsRequest facts) {
    }

    public record ActivateRequest(@NotBlank String reason) {
    }
}
