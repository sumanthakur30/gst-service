package com.shopmanagement.gstservice.compliance;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP adapter settings for live GSP when {@code gst.*.provider=http}.
 * Mock remains default ({@code gst.*.provider=mock}).
 */
@ConfigurationProperties(prefix = "gst.gsp")
public class GspClientProperties {

    /** GSP adapter base URL (no trailing slash). Blank = not configured. */
    private String baseUrl = "";

    private String apiKey = "";

    private String einvoicePath = "/einvoice/generate";

    private String ewayPath = "/eway/generate";

    private String einvoiceCancelPath = "/einvoice/cancel";

    private String ewayCancelPath = "/eway/cancel";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 30000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEinvoicePath() {
        return einvoicePath;
    }

    public void setEinvoicePath(String einvoicePath) {
        this.einvoicePath = einvoicePath;
    }

    public String getEwayPath() {
        return ewayPath;
    }

    public void setEwayPath(String ewayPath) {
        this.ewayPath = ewayPath;
    }

    public String getEinvoiceCancelPath() {
        return einvoiceCancelPath;
    }

    public void setEinvoiceCancelPath(String einvoiceCancelPath) {
        this.einvoiceCancelPath = einvoiceCancelPath;
    }

    public String getEwayCancelPath() {
        return ewayCancelPath;
    }

    public void setEwayCancelPath(String ewayCancelPath) {
        this.ewayCancelPath = ewayCancelPath;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    public String normalizedBaseUrl() {
        if (baseUrl == null) {
            return "";
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
