package com.shopmanagement.gstservice.service;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.api.GstApi.GstinDetailsResponse;
import com.shopmanagement.gstservice.api.GstApi.GstinValidateResponse;
import com.shopmanagement.gstservice.exception.GstinLookupException;
import com.shopmanagement.gstservice.exception.NotFoundException;
import com.shopmanagement.gstservice.support.GstinValidator;

@Service
public class GstinDetailsLookupService {

    private static final String UNAVAILABLE =
            "Unable to fetch GST details right now. Please try again or enter the details manually.";

    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private long accessTokenExpiresAtMs;

    public GstinDetailsLookupService(
            @Value("${gst.verification.enabled:false}") boolean enabled,
            @Value("${gst.verification.base-url:}") String baseUrl,
            @Value("${gst.verification.api-key:}") String apiKey,
            @Value("${gst.verification.api-secret:}") String apiSecret,
            @Value("${gst.verification.connect-timeout-ms:4000}") int connectTimeoutMs,
            @Value("${gst.verification.read-timeout-ms:8000}") int readTimeoutMs,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiSecret = apiSecret == null ? "" : apiSecret.trim();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
        this.objectMapper = objectMapper;
    }

    public GstinDetailsResponse lookup(String rawGstin) {
        GstinValidateResponse validation = GstinValidator.validate(rawGstin);
        if (!validation.validFormat() || validation.normalizedGstin() == null) {
            throw new GstinLookupException(HttpStatus.BAD_REQUEST, "Please enter a valid GSTIN.");
        }
        String gstin = validation.normalizedGstin();
        if (!isProviderConfigured()) {
            return partialFromGstin(gstin, validation.stateCode());
        }
        try {
            return searchWithTokenRefresh(gstin, validation.stateCode());
        } catch (GstinLookupException | NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GstinLookupException(HttpStatus.BAD_GATEWAY, UNAVAILABLE, ex);
        }
    }

    private GstinDetailsResponse searchWithTokenRefresh(String gstin, String stateCode) {
        try {
            return search(gstin, stateCode, token());
        } catch (GstinLookupException ex) {
            if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
                invalidateToken();
                return search(gstin, stateCode, token());
            }
            throw ex;
        }
    }

    private GstinDetailsResponse search(String gstin, String stateCode, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set(HttpHeaders.AUTHORIZATION, token);
        headers.set("x-api-version", "1.0");
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    trimSlash(baseUrl) + "/gst/compliance/public/gstin/search",
                    new HttpEntity<>(java.util.Map.of("gstin", gstin), headers),
                    JsonNode.class);
            JsonNode body = response.getBody();
            JsonNode payload = body == null ? null : body.path("data");
            JsonNode providerError = payload == null ? null : payload.path("error");
            if (providerError != null && !providerError.isMissingNode() && !providerError.isNull()) {
                throw new NotFoundException("GST details could not be found for this GSTIN.");
            }
            JsonNode taxpayer = payload == null ? null : payload.path("data");
            if (taxpayer == null || taxpayer.isMissingNode() || taxpayer.isNull() || taxpayer.isEmpty()) {
                throw new NotFoundException("GST details could not be found for this GSTIN.");
            }
            return map(gstin, stateCode, taxpayer);
        } catch (NotFoundException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new GstinLookupException(HttpStatus.UNAUTHORIZED, UNAVAILABLE, ex);
            }
            if (ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new GstinLookupException(HttpStatus.BAD_REQUEST, "Please enter a valid GSTIN.", ex);
            }
            throw new GstinLookupException(HttpStatus.BAD_GATEWAY, UNAVAILABLE, ex);
        } catch (ResourceAccessException ex) {
            throw new GstinLookupException(HttpStatus.GATEWAY_TIMEOUT, UNAVAILABLE, ex);
        }
    }

    private GstinDetailsResponse map(String gstin, String stateCode, JsonNode taxpayer) {
        String legalName = text(taxpayer, "lgnm");
        String tradeName = text(taxpayer, "tradeNam");
        if (isBlank(legalName) && isBlank(tradeName)) {
            throw new NotFoundException("GST details could not be found for this GSTIN.");
        }
        JsonNode address = taxpayer.path("pradr").path("addr");
        String city = firstNonBlank(text(address, "dst"), text(address, "loc"));
        String state = firstNonBlank(text(address, "stcd"), stateNameHint(stateCode));
        String pincode = text(address, "pncd");
        String line1 = join(
                text(address, "bno"),
                text(address, "flno"),
                text(address, "bnm"));
        String line2 = join(
                text(address, "st"),
                text(address, "loc"),
                text(address, "landMark"));
        String customerName = firstNonBlank(tradeName, legalName);
        return new GstinDetailsResponse(
                gstin,
                legalName,
                tradeName,
                customerName,
                gstin.substring(2, 12),
                text(taxpayer, "rgdt"),
                text(taxpayer, "dty"),
                text(taxpayer, "sts"),
                line1,
                line2,
                city,
                state,
                stateCode,
                pincode,
                text(taxpayer, "ctb"),
                true);
    }

    private GstinDetailsResponse partialFromGstin(String gstin, String stateCode) {
        return new GstinDetailsResponse(
                gstin,
                null,
                null,
                null,
                gstin.substring(2, 12),
                null,
                null,
                "PARTIAL",
                null,
                null,
                null,
                stateNameHint(stateCode),
                stateCode,
                null,
                null,
                false);
    }

    private synchronized String token() {
        if (accessToken != null && System.currentTimeMillis() < accessTokenExpiresAtMs) {
            return accessToken;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("x-api-secret", apiSecret);
        headers.set("x-api-version", "1.0.0");
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    trimSlash(baseUrl) + "/authenticate",
                    new HttpEntity<>(null, headers),
                    JsonNode.class);
            JsonNode body = response.getBody();
            String token = body == null ? null : text(body.path("data"), "access_token");
            if (isBlank(token)) {
                throw new GstinLookupException(HttpStatus.BAD_GATEWAY, UNAVAILABLE);
            }
            accessToken = token;
            accessTokenExpiresAtMs = System.currentTimeMillis() + Duration.ofHours(23).toMillis();
            return accessToken;
        } catch (GstinLookupException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GstinLookupException(HttpStatus.BAD_GATEWAY, UNAVAILABLE, ex);
        }
    }

    private synchronized void invalidateToken() {
        accessToken = null;
        accessTokenExpiresAtMs = 0L;
    }

    private boolean isProviderConfigured() {
        return enabled && !baseUrl.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    private static String stateNameHint(String stateCode) {
        if (stateCode == null) {
            return null;
        }
        return switch (stateCode) {
            case "04" -> "Chandigarh";
            case "06" -> "Haryana";
            case "07" -> "Delhi";
            case "09" -> "Uttar Pradesh";
            case "27" -> "Maharashtra";
            case "29" -> "Karnataka";
            case "33" -> "Tamil Nadu";
            case "36" -> "Telangana";
            default -> null;
        };
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.path(field).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String join(String... parts) {
        String joined = Stream.of(parts == null ? new String[0] : parts)
                .filter(part -> !isBlank(part))
                .map(String::trim)
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
