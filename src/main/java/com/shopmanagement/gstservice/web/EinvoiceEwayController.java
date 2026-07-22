package com.shopmanagement.gstservice.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.compliance.ComplianceProviderStatus;
import com.shopmanagement.gstservice.model.EinvoiceRequest;
import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.service.EinvoiceEwayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/gst")
@Tag(name = "GST E-Invoice / E-Way")
public class EinvoiceEwayController {

    private final EinvoiceEwayService service;

    public EinvoiceEwayController(EinvoiceEwayService service) {
        this.service = service;
    }

    public record GenerateEinvoiceRequest(@NotNull Long taxDocumentSnapshotId) {
    }

    public record GenerateEwayRequest(
            @NotNull Long taxDocumentSnapshotId,
            Integer distanceKm,
            String vehicleNo,
            String transporterId,
            String transporterName) {
    }

    public record CancelRequest(String reason) {
    }

    @GetMapping("/compliance/provider-status")
    @Operation(summary = "Active e-invoice / e-way provider mode (mock default)")
    public ComplianceProviderStatus providerStatus() {
        return service.providerStatus();
    }

    @PostMapping("/einvoice/generate")
    @Operation(summary = "Generate IRN for a posted tax snapshot (mock provider by default)")
    public EinvoiceRequest generateEinvoice(@Valid @RequestBody GenerateEinvoiceRequest body) {
        return service.generateEinvoice(body.taxDocumentSnapshotId());
    }

    @PostMapping("/einvoice/by-snapshot/{snapshotId}/cancel")
    @Operation(summary = "Cancel IRN for a snapshot (mock or live GSP)")
    public EinvoiceRequest cancelEinvoice(
            @PathVariable Long snapshotId, @RequestBody(required = false) CancelRequest body) {
        return service.cancelEinvoice(snapshotId, body != null ? body.reason() : null);
    }

    @GetMapping("/einvoice/by-snapshot/{snapshotId}")
    public EinvoiceRequest getEinvoice(@PathVariable Long snapshotId) {
        return service.getEinvoiceBySnapshot(snapshotId);
    }

    @PostMapping("/eway/generate")
    @Operation(summary = "Generate e-way bill for a posted tax snapshot (mock provider by default)")
    public EwayBillRequest generateEway(@Valid @RequestBody GenerateEwayRequest body) {
        return service.generateEway(
                body.taxDocumentSnapshotId(),
                body.distanceKm(),
                body.vehicleNo(),
                body.transporterId(),
                body.transporterName());
    }

    @PostMapping("/eway/by-snapshot/{snapshotId}/cancel")
    @Operation(summary = "Cancel e-way bill for a snapshot (mock or live GSP)")
    public EwayBillRequest cancelEway(
            @PathVariable Long snapshotId, @RequestBody(required = false) CancelRequest body) {
        return service.cancelEway(snapshotId, body != null ? body.reason() : null);
    }

    @GetMapping("/eway/by-snapshot/{snapshotId}")
    public EwayBillRequest getEway(@PathVariable Long snapshotId) {
        return service.getEwayBySnapshot(snapshotId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Bad request"));
    }
}
