package com.shopmanagement.gstservice.compliance;

import java.time.LocalDateTime;

/** Result from an e-invoice provider (mock or NIC/GSP). */
public record IrnResult(
        String irn,
        String ackNo,
        LocalDateTime ackDate,
        String signedQrPayload,
        String provider) {
}
