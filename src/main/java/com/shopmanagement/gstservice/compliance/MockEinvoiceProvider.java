package com.shopmanagement.gstservice.compliance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

/**
 * Sandbox e-invoice provider — no NIC credentials. Replace with NicGspEinvoiceProvider later.
 */
@Component
@ConditionalOnProperty(name = "gst.einvoice.provider", havingValue = "mock", matchIfMissing = true)
public class MockEinvoiceProvider implements EinvoiceProvider {

    @Override
    public IrnResult generate(TaxDocumentSnapshot snapshot) {
        String seed = snapshot.getTenantId() + ":" + snapshot.getId() + ":" + snapshot.getSourceId();
        String irn = "MOCK-IRN-" + sha16(seed).toUpperCase();
        String ack = String.valueOf(Math.abs(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits() % 1_000_000_000L));
        String qr = "mock-qr:" + irn;
        return new IrnResult(irn, ack, LocalDateTime.now(), qr, "mock");
    }

    private static String sha16(String value) {
        try {
            byte[] dig = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig).substring(0, 16);
        } catch (Exception ex) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
}
