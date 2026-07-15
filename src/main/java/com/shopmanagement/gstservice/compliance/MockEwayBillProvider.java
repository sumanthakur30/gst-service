package com.shopmanagement.gstservice.compliance;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

/**
 * Sandbox e-way provider — no NIC credentials. Replace with NicGspEwayBillProvider later.
 */
@Component
@ConditionalOnProperty(name = "gst.eway.provider", havingValue = "mock", matchIfMissing = true)
public class MockEwayBillProvider implements EwayBillProvider {

    @Override
    public EwayResult generate(TaxDocumentSnapshot snapshot, EwayBillRequest request) {
        long n = Math.abs((snapshot.getId() == null ? 0L : snapshot.getId()) * 1_000_003L
                + (request.getDistanceKm() == null ? 0 : request.getDistanceKm()));
        String ewb = String.format("%012d", n % 1_000_000_000_000L);
        int days = request.getDistanceKm() != null && request.getDistanceKm() > 200 ? 3 : 1;
        return new EwayResult(ewb, LocalDateTime.now().plusDays(days), "mock");
    }
}
