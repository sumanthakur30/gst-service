package com.shopmanagement.gstservice.compliance;

/** Result of a cancel IRN / e-way call. */
public record CancelResult(boolean cancelled, String provider, String message) {
}
