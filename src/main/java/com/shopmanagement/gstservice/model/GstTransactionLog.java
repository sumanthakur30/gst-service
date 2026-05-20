package com.shopmanagement.gstservice.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gst_transaction_log")
public class GstTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "request_id")
    private String requestId;

    @Column(nullable = false)
    private String operation;

    @Column(name = "source_service")
    private String sourceService;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "request_hash")
    private String requestHash;

    @Column(name = "request_json", columnDefinition = "TEXT")
    private String requestJson;

    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "calculation_ms")
    private Integer calculationMs;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static GstTransactionLog of(
            Long tenantId,
            String requestId,
            String operation,
            String sourceService,
            String sourceRef,
            String requestHash,
            String requestJson,
            String responseJson,
            int calculationMs) {
        GstTransactionLog log = new GstTransactionLog();
        log.tenantId = tenantId;
        log.requestId = requestId;
        log.operation = operation;
        log.sourceService = sourceService;
        log.sourceRef = sourceRef;
        log.requestHash = requestHash;
        log.requestJson = requestJson;
        log.responseJson = responseJson;
        log.calculationMs = calculationMs;
        return log;
    }
}
