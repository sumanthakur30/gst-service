package com.shopmanagement.gstservice.compliance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.model.GstRegistration;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;
import com.shopmanagement.gstservice.model.TaxLineSnapshot;
import com.shopmanagement.gstservice.repository.GstRegistrationRepository;

/**
 * Builds enriched GSP payloads (lines + seller GSTIN + supply type) for HTTP adapters.
 * Mock path does not use this; tax engine is untouched.
 */
@Component
public class GspPayloadFactory {

    private final GstRegistrationRepository registrationRepository;

    public GspPayloadFactory(GstRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public Map<String, Object> einvoiceBody(TaxDocumentSnapshot snapshot) {
        Map<String, Object> body = baseDocument(snapshot);
        body.put("documentType", snapshot.getDocumentType() != null ? snapshot.getDocumentType().name() : null);
        body.put("lines", lineRows(snapshot));
        return body;
    }

    public Map<String, Object> ewayBody(TaxDocumentSnapshot snapshot, EwayBillRequest request) {
        Map<String, Object> body = baseDocument(snapshot);
        body.put("lines", lineRows(snapshot));
        if (request != null) {
            body.put("distanceKm", request.getDistanceKm());
            body.put("transporterName", request.getTransporterName());
            body.put("transporterId", request.getTransporterId());
            body.put("vehicleNo", request.getVehicleNo());
        }
        return body;
    }

    public Map<String, Object> cancelEinvoiceBody(String irn, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("irn", irn);
        body.put("reason", reason != null && !reason.isBlank() ? reason.trim() : "Cancelled by user");
        return body;
    }

    public Map<String, Object> cancelEwayBody(String ewbNo, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ewbNo", ewbNo);
        body.put("reason", reason != null && !reason.isBlank() ? reason.trim() : "Cancelled by user");
        return body;
    }

    private Map<String, Object> baseDocument(TaxDocumentSnapshot snapshot) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", snapshot.getTenantId());
        body.put("snapshotId", snapshot.getId());
        body.put("sourceService", snapshot.getSourceService());
        body.put("sourceType", snapshot.getSourceType());
        body.put("sourceId", snapshot.getSourceId());
        body.put("sourceNumber", snapshot.getSourceNumber());
        body.put("documentDate", snapshot.getDocumentDate());
        body.put("placeOfSupplyState", snapshot.getPlaceOfSupplyState());
        body.put("supplyType", snapshot.getSupplyType() != null ? snapshot.getSupplyType().name() : null);
        body.put("buyerGstin", snapshot.getBuyerGstin());
        body.put("buyerStateCode", snapshot.getBuyerStateCode());
        body.put("sellerGstin", resolveSellerGstin(snapshot));
        body.put("sellerGstRegistrationId", snapshot.getSellerGstRegistrationId());
        body.put("taxableValue", snapshot.getTaxableValue());
        body.put("cgst", snapshot.getCgst());
        body.put("sgst", snapshot.getSgst());
        body.put("igst", snapshot.getIgst());
        body.put("cess", snapshot.getCess());
        body.put("grandTotal", snapshot.getGrandTotal());
        return body;
    }

    private List<Map<String, Object>> lineRows(TaxDocumentSnapshot snapshot) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (snapshot.getLines() == null) {
            return rows;
        }
        for (TaxLineSnapshot line : snapshot.getLines()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineNo", line.getLineNo());
            row.put("productId", line.getProductId());
            row.put("description", line.getDescription());
            row.put("hsnSac", line.getHsnSac());
            row.put("quantity", line.getQuantity());
            row.put("unitPrice", line.getUnitPrice());
            row.put("discount", line.getDiscount());
            row.put("taxableValue", line.getTaxableValue());
            row.put("gstRatePercent", line.getGstRatePercent());
            row.put("cgst", line.getCgst());
            row.put("sgst", line.getSgst());
            row.put("igst", line.getIgst());
            row.put("cess", line.getCess());
            row.put("lineTotal", line.getLineTotal());
            rows.add(row);
        }
        return rows;
    }

    private String resolveSellerGstin(TaxDocumentSnapshot snapshot) {
        if (snapshot.getSellerGstRegistrationId() == null) {
            return null;
        }
        return registrationRepository
                .findByTenantIdAndId(snapshot.getTenantId(), snapshot.getSellerGstRegistrationId())
                .map(GstRegistration::getGstin)
                .orElse(null);
    }
}
