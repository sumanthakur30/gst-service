package com.shopmanagement.gstservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.shopmanagement.gstservice.api.GstApi.TaxCalculateRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxLineRequest;
import com.shopmanagement.gstservice.engine.IndiaGstTaxEngine;

class IndiaGstTaxEngineTest {

    private final IndiaGstTaxEngine engine = new IndiaGstTaxEngine(18, 18);

    @Test
    void interState_usesIgst() {
        var result = engine.calculate(legacyRequest("27"));

        assertThat(result.interState()).isTrue();
        assertThat(result.igstAmount()).isGreaterThan(0);
        assertThat(result.cgstAmount()).isZero();
        assertThat(result.sgstAmount()).isZero();
    }

    @Test
    void intraState_splitsCgstSgst() {
        var result = engine.calculate(legacyRequest("29"));

        assertThat(result.interState()).isFalse();
        assertThat(result.cgstAmount()).isGreaterThan(0);
        assertThat(result.sgstAmount()).isGreaterThan(0);
        assertThat(result.igstAmount()).isZero();
    }

    private TaxCalculateRequest legacyRequest(String customerState) {
        return new TaxCalculateRequest(
                true,
                "29",
                customerState,
                null,
                null,
                0.0,
                null,
                List.of(new TaxLineRequest(1, 1L, null, 1.0, 118.0, 18.0, 0.0,
                        null, null, null, null, null, null, null, null)),
                null, null, null, null, null, null, null, null);
    }
}
