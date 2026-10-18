package com.shopmanagement.gstservice.compliance;

/**
 * Active GSP mode for UI / ops (mock remains default).
 * {@code readyForLive} is true when liveMode and base URL (+ optional api key policy) are set.
 */
public record ComplianceProviderStatus(
        String einvoiceProvider,
        String ewayProvider,
        boolean gspConfigured,
        boolean liveMode,
        boolean apiKeyPresent,
        boolean readyForLive,
        String checklistMessage) {
}
