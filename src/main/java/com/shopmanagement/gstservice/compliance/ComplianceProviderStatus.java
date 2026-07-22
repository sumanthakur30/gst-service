package com.shopmanagement.gstservice.compliance;

/**
 * Active GSP mode for UI / ops (mock remains default).
 */
public record ComplianceProviderStatus(
        String einvoiceProvider,
        String ewayProvider,
        boolean gspConfigured,
        boolean liveMode) {
}
