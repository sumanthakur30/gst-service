package com.shopmanagement.gstservice.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.api.GstApi.GstrFilingPackResponse;
import com.shopmanagement.gstservice.api.GstApi.GstrFilingUploadResponse;
import com.shopmanagement.gstservice.api.GstApi.GstrSummaryRequest;
import com.shopmanagement.gstservice.compliance.GspClientProperties;

@Service
public class GstFilingUploadService {

    private static final String DISCLAIMER =
            "GSTR upload goes only to the configured GSP adapter. SugamFlow never invents an ARN "
                    + "or marks a return filed without a provider acknowledgement.";

    private final GstComplianceService complianceService;
    private final GspClientProperties gsp;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GstFilingUploadService(
            GstComplianceService complianceService,
            GspClientProperties gsp,
            ObjectMapper objectMapper) {
        this.complianceService = complianceService;
        this.gsp = gsp;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, gsp.getConnectTimeoutMs())))
                .build();
    }

    public GstrFilingUploadResponse upload(GstrSummaryRequest request) {
        if (!gsp.isConfigured()) {
            return new GstrFilingUploadResponse(
                    false,
                    "NOT_CONFIGURED",
                    null,
                    "Set GST_GSP_BASE_URL to push GSTR-1/3B JSON to your GSP. Download the pack and upload on GSTN until then.",
                    DISCLAIMER);
        }
        GstrFilingPackResponse pack = complianceService.buildGstrFilingPack(request);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("returnPeriod", pack.returnPeriod());
            body.put("fromDate", pack.fromDate().toString());
            body.put("toDate", pack.toDate().toString());
            body.put("documentCount", pack.documentCount());
            body.put("gstr1FilingJson", pack.gstr1FilingJson());
            body.put("gstr3bFilingJson", pack.gstr3bFilingJson());
            JsonNode json = postJson(body);
            String arn = text(json, "arn", "ARN", "ackNo", "acknowledgementNumber");
            String message = text(json, "message", "status");
            if (arn == null || arn.isBlank()) {
                return new GstrFilingUploadResponse(
                        false,
                        "REJECTED",
                        null,
                        message != null ? message : "GSP response had no ARN — return is not marked filed.",
                        DISCLAIMER);
            }
            return new GstrFilingUploadResponse(true, "ACCEPTED", arn, message != null ? message : "Accepted", DISCLAIMER);
        } catch (Exception ex) {
            return new GstrFilingUploadResponse(
                    false,
                    "FAILED",
                    null,
                    ex.getMessage() != null ? ex.getMessage() : "GSP filing call failed",
                    DISCLAIMER);
        }
    }

    private JsonNode postJson(Map<String, Object> body) throws Exception {
        String path = gsp.getFilingPath();
        String url = gsp.normalizedBaseUrl()
                + (path == null || path.isBlank() ? "" : (path.startsWith("/") ? path : "/" + path));
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(1000, gsp.getReadTimeoutMs())))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (gsp.getApiKey() != null && !gsp.getApiKey().isBlank()) {
            builder.header("X-Api-Key", gsp.getApiKey().trim());
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GSP filing HTTP " + response.statusCode());
        }
        String raw = response.body() == null || response.body().isBlank() ? "{}" : response.body();
        return objectMapper.readTree(raw);
    }

    private static String text(JsonNode json, String... keys) {
        for (String key : keys) {
            JsonNode n = json.get(key);
            if (n != null && !n.isNull() && n.isValueNode()) {
                String v = n.asText();
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return null;
    }
}
