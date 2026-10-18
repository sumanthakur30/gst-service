package com.shopmanagement.gstservice.compliance;

/** Result from e-way Part-B update (mock or GSP). */
public record PartBResult(
        boolean success,
        String provider,
        String providerStatus,
        String message) {
}
