package com.shopmanagement.gstservice.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.api.GstApi.GstrSummaryRequest;
import com.shopmanagement.gstservice.api.GstApi.GstrSummaryResponse;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;
import com.shopmanagement.gstservice.repository.TaxDocumentSnapshotRepository;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class GstComplianceService {

    private final TaxDocumentSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public GstComplianceService(
            TaxDocumentSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    public GstrSummaryResponse buildGstrSummary(GstrSummaryRequest request) {
        long tenantId = TenantIds.require();
        List<TaxDocumentSnapshot> snapshots = snapshotRepository
                .findByTenantIdAndDocumentDateBetweenOrderByDocumentDateDescIdDesc(
                        tenantId, request.fromDate(), request.toDate());

        double taxable = 0;
        double cgst = 0;
        double sgst = 0;
        double igst = 0;
        double cess = 0;
        for (TaxDocumentSnapshot snap : snapshots) {
            taxable += snap.getTaxableValue().doubleValue();
            cgst += snap.getCgst().doubleValue();
            sgst += snap.getSgst().doubleValue();
            igst += snap.getIgst().doubleValue();
            if (snap.getCess() != null) {
                cess += snap.getCess().doubleValue();
            }
        }

        String gstr1 = toJson(new Gstr1Bucket(snapshots.size(), taxable, cgst, sgst, igst));
        String gstr3b = toJson(new Gstr3bBucket(taxable, cgst + sgst + igst, cess));
        String hsn = toJson(new HsnSummaryPlaceholder(snapshots.size()));

        return new GstrSummaryResponse(
                request.fromDate(),
                request.toDate(),
                taxable,
                cgst,
                sgst,
                igst,
                cess,
                snapshots.size(),
                gstr1,
                gstr3b,
                hsn);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private record Gstr1Bucket(int documentCount, double taxableValue, double cgst, double sgst, double igst) {
    }

    private record Gstr3bBucket(double taxableValue, double totalTax, double cess) {
    }

    private record HsnSummaryPlaceholder(int lineCountHint) {
    }
}
