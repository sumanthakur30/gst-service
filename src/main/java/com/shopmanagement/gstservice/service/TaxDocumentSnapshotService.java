package com.shopmanagement.gstservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.api.GstApi.CreditNotePostRequest;
import com.shopmanagement.gstservice.api.GstApi.InvoiceNumberResponse;
import com.shopmanagement.gstservice.api.GstApi.TaxCalculateRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxCalculateResponse;
import com.shopmanagement.gstservice.api.GstApi.TaxDocumentPostRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxDocumentSnapshotResponse;
import com.shopmanagement.gstservice.api.GstApi.TaxLineRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxLineResult;
import com.shopmanagement.gstservice.api.GstApi.TaxLineSnapshotResponse;
import com.shopmanagement.gstservice.exception.ConflictException;
import com.shopmanagement.gstservice.exception.NotFoundException;
import com.shopmanagement.gstservice.model.DocumentType;
import com.shopmanagement.gstservice.model.SupplyType;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;
import com.shopmanagement.gstservice.model.TaxLineSnapshot;
import com.shopmanagement.gstservice.repository.GstRegistrationBranchMapRepository;
import com.shopmanagement.gstservice.repository.TaxDocumentSnapshotRepository;
import com.shopmanagement.gstservice.support.SnapshotHasher;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class TaxDocumentSnapshotService {

    private final TaxDocumentSnapshotRepository snapshotRepository;
    private final GstRegistrationBranchMapRepository branchMapRepository;
    private final TaxCalculationService taxCalculationService;
    private final InvoiceSeriesService invoiceSeriesService;
    private final ObjectMapper objectMapper;

    public TaxDocumentSnapshotService(
            TaxDocumentSnapshotRepository snapshotRepository,
            GstRegistrationBranchMapRepository branchMapRepository,
            TaxCalculationService taxCalculationService,
            InvoiceSeriesService invoiceSeriesService,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.branchMapRepository = branchMapRepository;
        this.taxCalculationService = taxCalculationService;
        this.invoiceSeriesService = invoiceSeriesService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TaxDocumentSnapshotResponse get(Long id) {
        long tenantId = TenantIds.require();
        TaxDocumentSnapshot snap = snapshotRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Tax document snapshot not found"));
        return toResponse(snap);
    }

    @Transactional(readOnly = true)
    public List<TaxDocumentSnapshotResponse> list(LocalDate from, LocalDate to) {
        long tenantId = TenantIds.require();
        LocalDate start = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate end = to != null ? to : LocalDate.now();
        return snapshotRepository
                .findByTenantIdAndDocumentDateBetweenOrderByDocumentDateDescIdDesc(tenantId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaxDocumentSnapshotResponse post(TaxDocumentPostRequest request) {
        long tenantId = TenantIds.require();
        var existing = snapshotRepository.findByTenantIdAndSourceServiceAndSourceTypeAndSourceIdAndDocumentType(
                tenantId,
                request.sourceService(),
                request.sourceType(),
                request.sourceId(),
                request.documentType());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        TaxCalculateResponse tax = taxCalculationService.calculate(request.tax());
        Long sellerRegId = resolveSellerRegistration(request, tenantId);

        String sourceNumber = request.sourceNumber();
        boolean assignNumber = Boolean.TRUE.equals(request.assignInvoiceNumber())
                || request.documentType() == DocumentType.TAX_INVOICE;
        if (assignNumber && sellerRegId != null
                && (sourceNumber == null || sourceNumber.isBlank())) {
            InvoiceNumberResponse inv = invoiceSeriesService.nextNumber(
                    sellerRegId,
                    request.documentDate(),
                    request.invoiceSeriesPrefix());
            sourceNumber = inv.invoiceNumber();
        }

        TaxDocumentSnapshot snap = buildSnapshot(
                tenantId,
                request.sourceService(),
                request.sourceType(),
                request.sourceId(),
                sourceNumber,
                request.documentType(),
                request.documentDate(),
                sellerRegId,
                request.buyerGstin(),
                request.buyerStateCode() != null ? request.buyerStateCode() : request.tax().customerStateCode(),
                request.placeOfSupplyState() != null
                        ? request.placeOfSupplyState()
                        : request.tax().customerStateCode(),
                request.supplyType() != null ? request.supplyType()
                        : (request.buyerGstin() != null && !request.buyerGstin().isBlank()
                                ? SupplyType.B2B
                                : SupplyType.B2C),
                tax,
                request.tax().lines(),
                null);

        snap = snapshotRepository.save(snap);
        return toResponse(snap);
    }

    @Transactional
    public TaxDocumentSnapshotResponse postCreditNote(CreditNotePostRequest request) {
        long tenantId = TenantIds.require();
        TaxDocumentSnapshot original = snapshotRepository.findByTenantIdAndId(tenantId, request.originalSnapshotId())
                .orElseThrow(() -> new NotFoundException("Original tax snapshot not found"));

        var existing = snapshotRepository.findByTenantIdAndSourceServiceAndSourceTypeAndSourceIdAndDocumentType(
                tenantId,
                request.sourceService(),
                request.sourceType(),
                request.sourceId(),
                DocumentType.CREDIT_NOTE);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        double returnAmount = request.returnAmount();
        if (returnAmount <= 0) {
            throw new IllegalArgumentException("returnAmount must be positive; stored as negative on credit note");
        }

        double ratio = returnAmount / original.getGrandTotal().doubleValue();
        if (ratio > 1.0) {
            throw new IllegalArgumentException("returnAmount cannot exceed original grand total");
        }

        TaxCalculateResponse scaled = scaleFromOriginal(original, ratio);
        List<TaxLineRequest> lineRequests = List.of();

        TaxDocumentSnapshot snap = buildSnapshot(
                tenantId,
                request.sourceService(),
                request.sourceType(),
                request.sourceId(),
                request.sourceNumber(),
                DocumentType.CREDIT_NOTE,
                request.documentDate(),
                original.getSellerGstRegistrationId(),
                original.getBuyerGstin(),
                original.getBuyerStateCode(),
                original.getPlaceOfSupplyState(),
                original.getSupplyType(),
                scaled,
                lineRequests,
                original.getId());

        snap = snapshotRepository.save(snap);
        return toResponse(snap);
    }

    private TaxCalculateResponse scaleFromOriginal(TaxDocumentSnapshot original, double ratio) {
        return new TaxCalculateResponse(
                original.getTotalTax().compareTo(BigDecimal.ZERO) > 0,
                original.getIgst().compareTo(BigDecimal.ZERO) > 0,
                negate(original.getSubtotal(), ratio),
                negate(original.getDiscount(), ratio),
                negate(original.getTaxableValue(), ratio),
                negate(original.getTotalTax(), ratio),
                negate(original.getCgst(), ratio),
                negate(original.getSgst(), ratio),
                negate(original.getIgst(), ratio),
                negate(original.getGrandTotal(), ratio),
                List.of(),
                0.0,
                0.0,
                original.getBusinessType(),
                List.of());
    }

    private double negate(BigDecimal value, double ratio) {
        return round2(value.doubleValue() * ratio * -1.0);
    }

    private Long resolveSellerRegistration(TaxDocumentPostRequest request, long tenantId) {
        if (request.sellerGstRegistrationId() != null) {
            return request.sellerGstRegistrationId();
        }
        if (request.shopId() != null && !request.shopId().isBlank()) {
            return branchMapRepository.findByTenantIdAndShopId(tenantId, request.shopId().trim())
                    .map(m -> m.getGstRegistrationId())
                    .orElse(null);
        }
        return null;
    }

    private TaxDocumentSnapshot buildSnapshot(
            long tenantId,
            String sourceService,
            String sourceType,
            String sourceId,
            String sourceNumber,
            DocumentType documentType,
            LocalDate documentDate,
            Long sellerRegId,
            String buyerGstin,
            String buyerStateCode,
            String placeOfSupply,
            SupplyType supplyType,
            TaxCalculateResponse tax,
            List<TaxLineRequest> lineRequests,
            Long originalSnapshotId) {

        TaxDocumentSnapshot snap = new TaxDocumentSnapshot();
        snap.setTenantId(tenantId);
        snap.setSourceService(sourceService);
        snap.setSourceType(sourceType);
        snap.setSourceId(sourceId);
        snap.setSourceNumber(sourceNumber);
        snap.setDocumentType(documentType);
        snap.setDocumentDate(documentDate);
        snap.setSellerGstRegistrationId(sellerRegId);
        snap.setBuyerGstin(buyerGstin);
        snap.setBuyerStateCode(buyerStateCode);
        snap.setPlaceOfSupplyState(placeOfSupply);
        snap.setSupplyType(supplyType);
        snap.setSubtotal(bd(tax.subtotalAmount()));
        snap.setDiscount(bd(tax.discountAmount()));
        snap.setTaxableValue(bd(tax.taxableAmount()));
        snap.setTotalTax(bd(tax.taxAmount()));
        snap.setCgst(bd(tax.cgstAmount()));
        snap.setSgst(bd(tax.sgstAmount()));
        snap.setIgst(bd(tax.igstAmount()));
        snap.setRoundOff(BigDecimal.ZERO);
        snap.setGrandTotal(bd(tax.totalAmount()));
        snap.setOriginalSnapshotId(originalSnapshotId);

        List<TaxLineSnapshot> lines = new ArrayList<>();
        if (tax.lines() != null && !tax.lines().isEmpty()) {
            boolean interState = tax.interState();
            for (TaxLineResult line : tax.lines()) {
                TaxLineRequest req = findLineRequest(lineRequests, line.lineNo());
                TaxLineSnapshot row = new TaxLineSnapshot();
                row.setTaxDocumentSnapshot(snap);
                row.setLineNo(line.lineNo());
                if (req != null) {
                    row.setProductId(req.productId());
                    row.setHsnSac(req.hsnSac());
                    row.setQuantity(bd(req.quantity()));
                    row.setUnitPrice(bd(req.unitPrice()));
                    row.setDiscount(bd(safe(req.discountAmount())));
                } else {
                    row.setQuantity(BigDecimal.ONE);
                    row.setUnitPrice(bd(line.lineTotal()));
                }
                row.setTaxableValue(bd(line.taxableValue()));
                row.setGstRatePercent(line.gstPercentApplied() != null ? bd(line.gstPercentApplied()) : null);
                double half = interState ? 0.0 : line.taxAmount() / 2.0;
                double igst = interState ? line.taxAmount() : 0.0;
                row.setCgst(bd(half));
                row.setSgst(bd(half));
                row.setIgst(bd(igst));
                row.setLineTotal(bd(line.lineTotal()));
                row.setTaxInclusive(line.taxInclusive());
                lines.add(row);
            }
        }
        snap.setLines(lines);

        try {
            String json = objectMapper.writeValueAsString(tax);
            snap.setTaxSummaryJson(json);
            snap.setSnapshotHash(SnapshotHasher.sha256(json));
        } catch (JsonProcessingException ex) {
            throw new ConflictException("Failed to serialize tax summary");
        }
        return snap;
    }

    private TaxLineRequest findLineRequest(List<TaxLineRequest> requests, int lineNo) {
        if (requests == null) {
            return null;
        }
        return requests.stream().filter(r -> r.lineNo() == lineNo).findFirst().orElse(null);
    }

    private TaxDocumentSnapshotResponse toResponse(TaxDocumentSnapshot snap) {
        List<TaxLineSnapshotResponse> lines = snap.getLines() == null ? List.of()
                : snap.getLines().stream()
                        .map(l -> new TaxLineSnapshotResponse(
                                l.getLineNo(),
                                l.getProductId(),
                                l.getDescription(),
                                l.getHsnSac(),
                                l.getQuantity().doubleValue(),
                                l.getUnitPrice().doubleValue(),
                                l.getDiscount().doubleValue(),
                                l.getTaxableValue().doubleValue(),
                                l.getGstRatePercent() != null ? l.getGstRatePercent().doubleValue() : null,
                                l.getCgst().doubleValue(),
                                l.getSgst().doubleValue(),
                                l.getIgst().doubleValue(),
                                l.getLineTotal().doubleValue(),
                                l.isTaxInclusive()))
                        .toList();
        return new TaxDocumentSnapshotResponse(
                snap.getId(),
                snap.getSourceService(),
                snap.getSourceType(),
                snap.getSourceId(),
                snap.getSourceNumber(),
                snap.getDocumentType(),
                snap.getDocumentDate(),
                snap.getSellerGstRegistrationId(),
                snap.getBuyerGstin(),
                snap.getSupplyType(),
                snap.getSubtotal().doubleValue(),
                snap.getDiscount().doubleValue(),
                snap.getTaxableValue().doubleValue(),
                snap.getTotalTax().doubleValue(),
                snap.getCgst().doubleValue(),
                snap.getSgst().doubleValue(),
                snap.getIgst().doubleValue(),
                snap.getGrandTotal().doubleValue(),
                snap.getSnapshotHash(),
                snap.getOriginalSnapshotId(),
                lines);
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
