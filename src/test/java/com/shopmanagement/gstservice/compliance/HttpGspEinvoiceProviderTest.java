package com.shopmanagement.gstservice.compliance;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;
import com.shopmanagement.gstservice.repository.GstRegistrationRepository;

class HttpGspEinvoiceProviderTest {

    @Test
    void failsClearlyWhenBaseUrlMissing() {
        GspClientProperties props = new GspClientProperties();
        props.setBaseUrl("");
        GspPayloadFactory payloadFactory = new GspPayloadFactory(mock(GstRegistrationRepository.class));
        HttpGspEinvoiceProvider provider = new HttpGspEinvoiceProvider(props, new ObjectMapper(), payloadFactory);
        TaxDocumentSnapshot snapshot = new TaxDocumentSnapshot();
        snapshot.setTenantId(1L);
        snapshot.setSourceId("x");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.generate(snapshot));
        assertTrue(ex.getMessage().contains("gst.gsp.base-url"));
    }
}
