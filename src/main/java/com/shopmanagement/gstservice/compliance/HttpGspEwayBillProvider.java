package com.shopmanagement.gstservice.compliance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

/**
 * Live GSP e-way hook. Active only when {@code gst.eway.provider=http}.
 * Requires {@code gst.gsp.base-url}; otherwise fails with a clear configuration error.
 * Does not replace {@link MockEwayBillProvider} (default).
 */
@Component
@ConditionalOnProperty(name = "gst.eway.provider", havingValue = "http")
public class HttpGspEwayBillProvider implements EwayBillProvider {

    private final GspClientProperties props;
    private final ObjectMapper objectMapper;
    private final GspPayloadFactory payloadFactory;
    private final HttpClient httpClient;

    public HttpGspEwayBillProvider(
            GspClientProperties props, ObjectMapper objectMapper, GspPayloadFactory payloadFactory) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.payloadFactory = payloadFactory;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, props.getConnectTimeoutMs())))
                .build();
    }

    @Override
    public EwayResult generate(TaxDocumentSnapshot snapshot, EwayBillRequest request) {
        requireConfigured();
        try {
            Map<String, Object> body = payloadFactory.ewayBody(snapshot, request);
            JsonNode json = postJson(props.getEwayPath(), body);
            String ewb = text(json, "ewbNo", "ewb_no", "ewayBillNo");
            LocalDateTime validUpto = parseDateTime(json, "validUpto", "valid_upto");
            if (ewb == null || ewb.isBlank()) {
                throw new IllegalStateException("GSP e-way response missing ewbNo");
            }
            return new EwayResult(
                    ewb,
                    validUpto != null ? validUpto : LocalDateTime.now().plusDays(1),
                    "http");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("GSP e-way call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CancelResult cancel(String ewbNo, String reason) {
        requireConfigured();
        if (ewbNo == null || ewbNo.isBlank()) {
            throw new IllegalArgumentException("EWB number is required to cancel");
        }
        try {
            JsonNode json = postJson(props.getEwayCancelPath(), payloadFactory.cancelEwayBody(ewbNo, reason));
            boolean ok = json.path("cancelled").asBoolean(true)
                    || "CANCELLED".equalsIgnoreCase(text(json, "status"));
            String message = text(json, "message", "status");
            return new CancelResult(ok, "http", message != null ? message : "Cancelled");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("GSP e-way cancel failed: " + ex.getMessage(), ex);
        }
    }

    private void requireConfigured() {
        if (!props.isConfigured()) {
            throw new IllegalStateException(
                    "GSP not configured: set gst.gsp.base-url when gst.eway.provider=http");
        }
    }

    private JsonNode postJson(String path, Map<String, Object> body) throws IOException, InterruptedException {
        String url = props.normalizedBaseUrl()
                + (path == null || path.isBlank()
                        ? ""
                        : (path.startsWith("/") ? path : "/" + path));
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(1000, props.getReadTimeoutMs())))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            builder.header("X-Api-Key", props.getApiKey().trim());
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "GSP e-way HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }
        return objectMapper.readTree(response.body() == null || response.body().isBlank() ? "{}" : response.body());
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

    private static LocalDateTime parseDateTime(JsonNode json, String... keys) {
        String raw = text(json, keys);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }
}
