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
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

/**
 * Live GSP e-invoice hook. Active only when {@code gst.einvoice.provider=http}.
 * Requires {@code gst.gsp.base-url}; otherwise fails with a clear configuration error.
 * Does not replace {@link MockEinvoiceProvider} (default).
 */
@Component
@ConditionalOnProperty(name = "gst.einvoice.provider", havingValue = "http")
public class HttpGspEinvoiceProvider implements EinvoiceProvider {

    private final GspClientProperties props;
    private final ObjectMapper objectMapper;
    private final GspPayloadFactory payloadFactory;
    private final HttpClient httpClient;

    public HttpGspEinvoiceProvider(
            GspClientProperties props, ObjectMapper objectMapper, GspPayloadFactory payloadFactory) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.payloadFactory = payloadFactory;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, props.getConnectTimeoutMs())))
                .build();
    }

    @Override
    public IrnResult generate(TaxDocumentSnapshot snapshot) {
        requireConfigured();
        try {
            Map<String, Object> body = payloadFactory.einvoiceBody(snapshot);
            JsonNode json = postJson(props.getEinvoicePath(), body);
            String irn = text(json, "irn");
            String ackNo = text(json, "ackNo", "ack_no");
            String qr = text(json, "signedQrPayload", "signed_qr", "qr");
            LocalDateTime ackDate = parseDateTime(json, "ackDate", "ack_date");
            if (irn == null || irn.isBlank()) {
                throw new IllegalStateException("GSP e-invoice response missing irn");
            }
            return new IrnResult(
                    irn,
                    ackNo != null ? ackNo : "",
                    ackDate != null ? ackDate : LocalDateTime.now(),
                    qr != null ? qr : "",
                    "http");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("GSP e-invoice call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CancelResult cancel(String irn, String reason) {
        requireConfigured();
        if (irn == null || irn.isBlank()) {
            throw new IllegalArgumentException("IRN is required to cancel");
        }
        try {
            JsonNode json = postJson(props.getEinvoiceCancelPath(), payloadFactory.cancelEinvoiceBody(irn, reason));
            boolean ok = json.path("cancelled").asBoolean(true)
                    || "CANCELLED".equalsIgnoreCase(text(json, "status"));
            String message = text(json, "message", "status");
            return new CancelResult(ok, "http", message != null ? message : "Cancelled");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("GSP e-invoice cancel failed: " + ex.getMessage(), ex);
        }
    }

    private void requireConfigured() {
        if (!props.isConfigured()) {
            throw new IllegalStateException(
                    "GSP not configured: set gst.gsp.base-url when gst.einvoice.provider=http");
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
                    "GSP e-invoice HTTP " + response.statusCode() + ": " + truncate(response.body()));
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
