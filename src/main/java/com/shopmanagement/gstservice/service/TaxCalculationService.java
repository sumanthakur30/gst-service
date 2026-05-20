package com.shopmanagement.gstservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.api.GstApi.TaxCalculateRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxCalculateResponse;
import com.shopmanagement.gstservice.engine.EnterpriseGstTaxOrchestrator;
import com.shopmanagement.gstservice.filter.RequestIdFilter;
import com.shopmanagement.gstservice.model.GstTransactionLog;
import com.shopmanagement.gstservice.repository.GstTransactionLogRepository;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class TaxCalculationService {

    private static final Logger log = LoggerFactory.getLogger(TaxCalculationService.class);

    private final EnterpriseGstTaxOrchestrator orchestrator;
    private final GstTransactionLogRepository transactionLogRepository;
    private final ObjectMapper objectMapper;

    public TaxCalculationService(
            EnterpriseGstTaxOrchestrator orchestrator,
            GstTransactionLogRepository transactionLogRepository,
            ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.transactionLogRepository = transactionLogRepository;
        this.objectMapper = objectMapper;
    }

    public TaxCalculateResponse calculate(TaxCalculateRequest request) {
        long started = System.currentTimeMillis();
        TaxCalculateResponse response = orchestrator.calculate(request);
        auditLog("CALCULATE", request, response, started);
        return response;
    }

    @Async
    public void calculateBulkAsync(Long tenantId, String batchId) {
        log.debug("Bulk GST calculation queued tenant={} batch={}", tenantId, batchId);
    }

    private void auditLog(String operation, TaxCalculateRequest request, TaxCalculateResponse response, long started) {
        try {
            Long tenantId = TenantIds.currentOrNull();
            if (tenantId == null) {
                return;
            }
            String requestJson = objectMapper.writeValueAsString(request);
            String responseJson = objectMapper.writeValueAsString(response);
            String hash = sha256(requestJson);
            GstTransactionLog entry = GstTransactionLog.of(
                    tenantId,
                    RequestIdFilter.getCurrentRequestId(),
                    operation,
                    "gst-service",
                    null,
                    hash,
                    requestJson,
                    responseJson,
                    (int) (System.currentTimeMillis() - started));
            transactionLogRepository.save(entry);
        } catch (Exception ex) {
            // Audit must never fail tax preview/calculate (e.g. gst_transaction_log not migrated yet).
            log.warn("Failed to audit GST calculation: {}", ex.getMessage());
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return null;
        }
    }
}
