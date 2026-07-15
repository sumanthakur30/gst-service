package com.shopmanagement.gstservice.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.shopmanagement.gstservice.model.EwayBillRequest;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

class MockComplianceProvidersTest {

    @Test
    void mockEinvoice_isDeterministicForSameSnapshot() {
        MockEinvoiceProvider provider = new MockEinvoiceProvider();
        TaxDocumentSnapshot snap = new TaxDocumentSnapshot();
        snap.setId(42L);
        snap.setTenantId(1L);
        snap.setSourceId("INV-1");

        IrnResult a = provider.generate(snap);
        IrnResult b = provider.generate(snap);
        assertEquals(a.irn(), b.irn());
        assertTrue(a.irn().startsWith("MOCK-IRN-"));
        assertEquals("mock", a.provider());
        assertNotNull(a.ackNo());
    }

    @Test
    void mockEway_returnsEwbNumber() {
        MockEwayBillProvider provider = new MockEwayBillProvider();
        TaxDocumentSnapshot snap = new TaxDocumentSnapshot();
        snap.setId(7L);
        snap.setTenantId(1L);
        EwayBillRequest req = new EwayBillRequest();
        req.setDistanceKm(120);
        req.setVehicleNo("MH12AB1234");

        EwayResult result = provider.generate(snap, req);
        assertNotNull(result.ewbNo());
        assertEquals(12, result.ewbNo().length());
        assertNotNull(result.validUpto());
        assertEquals("mock", result.provider());
    }
}
