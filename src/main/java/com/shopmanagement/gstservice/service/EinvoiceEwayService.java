package com.shopmanagement.gstservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanagement.gstservice.compliance.CancelResult;
import com.shopmanagement.gstservice.compliance.ComplianceProviderStatus;
import com.shopmanagement.gstservice.compliance.EinvoiceProvider;
import com.shopmanagement.gstservice.compliance.EwayBillProvider;
import com.shopmanagement.gstservice.compliance.EwayResult;
import com.shopmanagement.gstservice.compliance.GspClientProperties;
import com.shopmanagement.gstservice.compliance.IrnResult;
import com.shopmanagement.gstservice.exception.NotFoundException;
import com.shopmanagement.gstservice.model.EinvoiceRequest;
import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;
import com.shopmanagement.gstservice.repository.EinvoiceRequestRepository;
import com.shopmanagement.gstservice.repository.EwayBillRequestRepository;
import com.shopmanagement.gstservice.repository.TaxDocumentSnapshotRepository;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class EinvoiceEwayService {

    private final TaxDocumentSnapshotRepository snapshotRepository;
    private final EinvoiceRequestRepository einvoiceRequestRepository;
    private final EwayBillRequestRepository ewayBillRequestRepository;
    private final EinvoiceProvider einvoiceProvider;
    private final EwayBillProvider ewayBillProvider;
    private final GspClientProperties gspClientProperties;
    private final String einvoiceProviderName;
    private final String ewayProviderName;

    public EinvoiceEwayService(
            TaxDocumentSnapshotRepository snapshotRepository,
            EinvoiceRequestRepository einvoiceRequestRepository,
            EwayBillRequestRepository ewayBillRequestRepository,
            EinvoiceProvider einvoiceProvider,
            EwayBillProvider ewayBillProvider,
            GspClientProperties gspClientProperties,
            @Value("${gst.einvoice.provider:mock}") String einvoiceProviderName,
            @Value("${gst.eway.provider:mock}") String ewayProviderName) {
        this.snapshotRepository = snapshotRepository;
        this.einvoiceRequestRepository = einvoiceRequestRepository;
        this.ewayBillRequestRepository = ewayBillRequestRepository;
        this.einvoiceProvider = einvoiceProvider;
        this.ewayBillProvider = ewayBillProvider;
        this.gspClientProperties = gspClientProperties;
        this.einvoiceProviderName = einvoiceProviderName;
        this.ewayProviderName = ewayProviderName;
    }

    @Transactional(readOnly = true)
    public ComplianceProviderStatus providerStatus() {
        boolean live = "http".equalsIgnoreCase(einvoiceProviderName)
                || "http".equalsIgnoreCase(ewayProviderName);
        return new ComplianceProviderStatus(
                einvoiceProviderName,
                ewayProviderName,
                gspClientProperties.isConfigured(),
                live);
    }

    @Transactional
    public EinvoiceRequest generateEinvoice(Long taxDocumentSnapshotId) {
        if (taxDocumentSnapshotId == null) {
            throw new IllegalArgumentException("taxDocumentSnapshotId is required");
        }
        long tenantId = TenantIds.require();
        TaxDocumentSnapshot snapshot = requireSnapshot(tenantId, taxDocumentSnapshotId);

        return einvoiceRequestRepository.findByTenantIdAndTaxDocumentSnapshotId(tenantId, taxDocumentSnapshotId)
                .map(existing -> {
                    if ("IRN_GENERATED".equalsIgnoreCase(existing.getStatus()) && existing.getIrn() != null) {
                        return existing;
                    }
                    return runEinvoice(existing, snapshot);
                })
                .orElseGet(() -> {
                    EinvoiceRequest created = new EinvoiceRequest();
                    created.setTenantId(tenantId);
                    created.setTaxDocumentSnapshotId(taxDocumentSnapshotId);
                    created.setStatus("PENDING");
                    return runEinvoice(einvoiceRequestRepository.save(created), snapshot);
                });
    }

    @Transactional
    public EinvoiceRequest cancelEinvoice(Long taxDocumentSnapshotId, String reason) {
        long tenantId = TenantIds.require();
        EinvoiceRequest existing = einvoiceRequestRepository
                .findByTenantIdAndTaxDocumentSnapshotId(tenantId, taxDocumentSnapshotId)
                .orElseThrow(() -> new NotFoundException(
                        "E-invoice not generated yet for snapshot " + taxDocumentSnapshotId));
        if (!"IRN_GENERATED".equalsIgnoreCase(existing.getStatus()) || existing.getIrn() == null) {
            throw new IllegalArgumentException("Only generated IRNs can be cancelled");
        }
        try {
            CancelResult result = einvoiceProvider.cancel(existing.getIrn(), reason);
            existing.setStatus("CANCELLED");
            existing.setErrorCode(null);
            existing.setErrorMessage(result.message());
            existing.setProvider(result.provider());
            return einvoiceRequestRepository.save(existing);
        } catch (Exception ex) {
            existing.setErrorCode("CANCEL_ERROR");
            existing.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "Cancel failed");
            einvoiceRequestRepository.save(existing);
            throw new IllegalArgumentException("E-invoice cancel failed: " + existing.getErrorMessage());
        }
    }

    @Transactional(readOnly = true)
    public EinvoiceRequest getEinvoiceBySnapshot(Long taxDocumentSnapshotId) {
        long tenantId = TenantIds.require();
        return einvoiceRequestRepository.findByTenantIdAndTaxDocumentSnapshotId(tenantId, taxDocumentSnapshotId)
                .orElseThrow(() -> new NotFoundException(
                        "E-invoice not generated yet for snapshot " + taxDocumentSnapshotId));
    }

    @Transactional
    public EwayBillRequest generateEway(
            Long taxDocumentSnapshotId,
            Integer distanceKm,
            String vehicleNo,
            String transporterId,
            String transporterName) {
        if (taxDocumentSnapshotId == null) {
            throw new IllegalArgumentException("taxDocumentSnapshotId is required");
        }
        long tenantId = TenantIds.require();
        TaxDocumentSnapshot snapshot = requireSnapshot(tenantId, taxDocumentSnapshotId);

        return ewayBillRequestRepository.findByTenantIdAndTaxDocumentSnapshotId(tenantId, taxDocumentSnapshotId)
                .map(existing -> {
                    if ("GENERATED".equalsIgnoreCase(existing.getStatus()) && existing.getEwbNo() != null) {
                        return existing;
                    }
                    applyEwayInput(existing, distanceKm, vehicleNo, transporterId, transporterName);
                    return runEway(existing, snapshot);
                })
                .orElseGet(() -> {
                    EwayBillRequest created = new EwayBillRequest();
                    created.setTenantId(tenantId);
                    created.setTaxDocumentSnapshotId(taxDocumentSnapshotId);
                    created.setStatus("PENDING");
                    applyEwayInput(created, distanceKm, vehicleNo, transporterId, transporterName);
                    return runEway(ewayBillRequestRepository.save(created), snapshot);
                });
    }

    @Transactional
    public EwayBillRequest cancelEway(Long taxDocumentSnapshotId, String reason) {
        long tenantId = TenantIds.require();
        EwayBillRequest existing = ewayBillRequestRepository
                .findByTenantIdAndTaxDocumentSnapshotId(tenantId, taxDocumentSnapshotId)
                .orElseThrow(() -> new NotFoundException(
                        "E-way bill not generated yet for snapshot " + taxDocumentSnapshotId));
        if (!"GENERATED".equalsIgnoreCase(existing.getStatus()) || existing.getEwbNo() == null) {
            throw new IllegalArgumentException("Only generated e-way bills can be cancelled");
        }
        try {
            CancelResult result = ewayBillProvider.cancel(existing.getEwbNo(), reason);
            existing.setStatus("CANCELLED");
            existing.setErrorCode(null);
            existing.setErrorMessage(result.message());
            existing.setProvider(result.provider());
            return ewayBillRequestRepository.save(existing);
        } catch (Exception ex) {
            existing.setErrorCode("CANCEL_ERROR");
            existing.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "Cancel failed");
            ewayBillRequestRepository.save(existing);
            throw new IllegalArgumentException("E-way cancel failed: " + existing.getErrorMessage());
        }
    }

    @Transactional(readOnly = true)
    public EwayBillRequest getEwayBySnapshot(Long taxDocumentSnapshotId) {
        long tenantId = TenantIds.require();
        return ewayBillRequestRepository.findByTenantIdAndTaxDocumentSnapshotId(tenantId, taxDocumentSnapshotId)
                .orElseThrow(() -> new NotFoundException(
                        "E-way bill not generated yet for snapshot " + taxDocumentSnapshotId));
    }

    private EinvoiceRequest runEinvoice(EinvoiceRequest request, TaxDocumentSnapshot snapshot) {
        try {
            IrnResult result = einvoiceProvider.generate(snapshot);
            request.setProvider(result.provider());
            request.setIrn(result.irn());
            request.setAckNo(result.ackNo());
            request.setAckDate(result.ackDate());
            request.setSignedQrPayload(result.signedQrPayload());
            request.setStatus("IRN_GENERATED");
            request.setErrorCode(null);
            request.setErrorMessage(null);
            return einvoiceRequestRepository.save(request);
        } catch (Exception ex) {
            request.setStatus("FAILED");
            request.setErrorCode("PROVIDER_ERROR");
            request.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "E-invoice failed");
            request.setRetryCount(request.getRetryCount() + 1);
            einvoiceRequestRepository.save(request);
            throw new IllegalArgumentException("E-invoice generation failed: " + request.getErrorMessage());
        }
    }

    private EwayBillRequest runEway(EwayBillRequest request, TaxDocumentSnapshot snapshot) {
        try {
            EwayResult result = ewayBillProvider.generate(snapshot, request);
            request.setProvider(result.provider());
            request.setEwbNo(result.ewbNo());
            request.setValidUpto(result.validUpto());
            request.setStatus("GENERATED");
            request.setErrorCode(null);
            request.setErrorMessage(null);
            return ewayBillRequestRepository.save(request);
        } catch (Exception ex) {
            request.setStatus("FAILED");
            request.setErrorCode("PROVIDER_ERROR");
            request.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "E-way failed");
            request.setRetryCount(request.getRetryCount() + 1);
            ewayBillRequestRepository.save(request);
            throw new IllegalArgumentException("E-way generation failed: " + request.getErrorMessage());
        }
    }

    private static void applyEwayInput(
            EwayBillRequest request,
            Integer distanceKm,
            String vehicleNo,
            String transporterId,
            String transporterName) {
        if (distanceKm != null) {
            request.setDistanceKm(distanceKm);
        }
        if (vehicleNo != null && !vehicleNo.isBlank()) {
            request.setVehicleNo(vehicleNo.trim().toUpperCase());
        }
        if (transporterId != null && !transporterId.isBlank()) {
            request.setTransporterId(transporterId.trim());
        }
        if (transporterName != null && !transporterName.isBlank()) {
            request.setTransporterName(transporterName.trim());
        }
    }

    private TaxDocumentSnapshot requireSnapshot(long tenantId, Long snapshotId) {
        TaxDocumentSnapshot snapshot = snapshotRepository.findByTenantIdAndId(tenantId, snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Tax document snapshot not found: " + snapshotId));
        if (snapshot.getLines() != null) {
            snapshot.getLines().size();
        }
        return snapshot;
    }
}
