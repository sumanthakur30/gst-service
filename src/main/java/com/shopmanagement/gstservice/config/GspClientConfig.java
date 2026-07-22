package com.shopmanagement.gstservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.shopmanagement.gstservice.compliance.GspClientProperties;

/**
 * Fail-fast when live GSP is selected but {@code gst.gsp.base-url} is blank.
 * Mock mode is unchanged (default).
 */
@Configuration
@EnableConfigurationProperties(GspClientProperties.class)
@Order(50)
public class GspClientConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GspClientConfig.class);

    private final GspClientProperties gsp;
    private final String einvoiceProvider;
    private final String ewayProvider;
    private final boolean requireApiKey;

    public GspClientConfig(
            GspClientProperties gsp,
            @Value("${gst.einvoice.provider:mock}") String einvoiceProvider,
            @Value("${gst.eway.provider:mock}") String ewayProvider,
            @Value("${gst.gsp.require-api-key:false}") boolean requireApiKey) {
        this.gsp = gsp;
        this.einvoiceProvider = einvoiceProvider;
        this.ewayProvider = ewayProvider;
        this.requireApiKey = requireApiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean liveEinvoice = "http".equalsIgnoreCase(einvoiceProvider);
        boolean liveEway = "http".equalsIgnoreCase(ewayProvider);
        if (!liveEinvoice && !liveEway) {
            log.info("GSP mode: mock (gst.einvoice.provider={}, gst.eway.provider={})",
                    einvoiceProvider, ewayProvider);
            return;
        }
        if (!gsp.isConfigured()) {
            throw new IllegalStateException(
                    "Live GSP selected (provider=http) but gst.gsp.base-url / GST_GSP_BASE_URL is blank. "
                            + "Set the GSP adapter URL or switch GST_EINVOICE_PROVIDER / GST_EWAY_PROVIDER back to mock.");
        }
        if (requireApiKey && (gsp.getApiKey() == null || gsp.getApiKey().isBlank())) {
            throw new IllegalStateException(
                    "Live GSP requires gst.gsp.api-key / GST_GSP_API_KEY when gst.gsp.require-api-key=true.");
        }
        log.info(
                "GSP LIVE ready: baseUrl={} einvoice={} eway={} apiKeyPresent={}",
                gsp.normalizedBaseUrl(),
                einvoiceProvider,
                ewayProvider,
                gsp.getApiKey() != null && !gsp.getApiKey().isBlank());
    }
}
