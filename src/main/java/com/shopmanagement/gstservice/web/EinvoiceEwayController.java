package com.shopmanagement.gstservice.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.compliance.ComplianceProviderStatus;
import com.shopmanagement.gstservice.model.EinvoiceRequest;
import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.model.EwayPartBUpdate;
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

    public record PartBUpdateRequest(String vehicleNo, String fromPlace, String transDocNo) {
    }

    public record BulkSnapshotRequest(List<Long> snapshotIds, String reason) {
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

    @GetMapping("/einvoice")
    @Operation(summary = "List e-invoice requests for tenant, optional status filter")
    public List<EinvoiceRequest> listEinvoice(@RequestParam(required = false) String status) {
        return service.listEinvoice(status);
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

    @GetMapping("/eway")
    @Operation(summary = "List e-way bill requests for tenant, optional status filter")
    public List<EwayBillRequest> listEway(@RequestParam(required = false) String status) {
        return service.listEway(status);
    }

    @PostMapping("/eway/by-snapshot/{id}/part-b")
    @Operation(summary = "Update Part-B (vehicle / transporter) on generated e-way bill")
    public EwayPartBUpdate updatePartB(
            @PathVariable("id") Long snapshotId,
            @RequestBody PartBUpdateRequest body) {
        return service.updatePartB(
                snapshotId,
                body != null ? body.vehicleNo() : null,
                body != null ? body.fromPlace() : null,
                body != null ? body.transDocNo() : null);
    }

    @PostMapping("/eway/bulk-cancel")
    @Operation(summary = "Bulk cancel e-way bills by tax snapshot ids")
    public List<EwayBillRequest> bulkCancelEway(@RequestBody BulkSnapshotRequest body) {
        return service.bulkCancelEway(
                body != null ? body.snapshotIds() : List.of(),
                body != null ? body.reason() : null);
    }

    @PostMapping("/eway/bulk-retry")
    @Operation(summary = "Bulk retry failed e-way generation by tax snapshot ids")
    public List<EwayBillRequest> bulkRetryEway(@RequestBody BulkSnapshotRequest body) {
        return service.bulkRetryEway(body != null ? body.snapshotIds() : List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Bad request"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> gspState(IllegalStateException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "GSP / compliance error";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", msg));
    }
}
