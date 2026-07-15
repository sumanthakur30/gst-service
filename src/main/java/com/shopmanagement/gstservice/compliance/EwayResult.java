package com.shopmanagement.gstservice.compliance;

import java.time.LocalDateTime;

/** Result from an e-way bill provider (mock or NIC/GSP). */
public record EwayResult(
        String ewbNo,
        LocalDateTime validUpto,
        String provider) {
}
