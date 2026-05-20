package com.shopmanagement.gstservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.shopmanagement.gstservice.api.GstApi.TaxCalculateRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxCalculateResponse;
import com.shopmanagement.gstservice.api.GstApi.TaxLineRequest;
import com.shopmanagement.gstservice.engine.EnterpriseGstTaxOrchestrator;
import com.shopmanagement.gstservice.engine.IndiaGstTaxEngine;
import com.shopmanagement.gstservice.engine.factory.GstStrategyFactory;
import com.shopmanagement.gstservice.engine.pipeline.GstInvoiceCalculationPipeline;
import com.shopmanagement.gstservice.engine.strategy.DefaultGstStrategy;
import com.shopmanagement.gstservice.engine.strategy.DistributorGstStrategy;
import com.shopmanagement.gstservice.engine.strategy.ManufacturingGstStrategy;
import com.shopmanagement.gstservice.engine.strategy.MedicalGstStrategy;
import com.shopmanagement.gstservice.engine.strategy.RestaurantGstStrategy;
import com.shopmanagement.gstservice.engine.strategy.RetailGstStrategy;
import com.shopmanagement.gstservice.service.GstSlabResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnterpriseGstTaxEngineTest {

    @Mock
    private GstSlabResolver slabResolver;

    private EnterpriseGstTaxOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        when(slabResolver.resolveSlab(any(), eq("RESTAURANT_5")))
                .thenReturn(new GstSlabResolver.ResolvedSlab("RESTAURANT_5", 5.0, 0, "TAXABLE"));
        when(slabResolver.resolveSlab(any(), eq("RESTAURANT_18")))
                .thenReturn(new GstSlabResolver.ResolvedSlab("RESTAURANT_18", 18.0, 0, "TAXABLE"));

        var retail = new RetailGstStrategy(slabResolver);
        var restaurant = new RestaurantGstStrategy(slabResolver);
        var medical = new MedicalGstStrategy(slabResolver);
        var distributor = new DistributorGstStrategy(slabResolver);
        var manufacturing = new ManufacturingGstStrategy(slabResolver);
        var defaults = new DefaultGstStrategy(slabResolver);
        var factory = new GstStrategyFactory(
                List.of(retail, restaurant, medical, distributor, manufacturing, defaults),
                defaults);
        var pipeline = new GstInvoiceCalculationPipeline(factory, 18);
        orchestrator = new EnterpriseGstTaxOrchestrator(pipeline, new IndiaGstTaxEngine(18, 18));
    }

    @Test
    void restaurant_dineIn_applies5Percent() {
        TaxCalculateResponse result = orchestrator.calculate(restaurantRequest(Map.of("serviceMode", "DINE_IN")));
        assertThat(result.taxAmount()).isGreaterThan(0);
        assertThat(result.lines().get(0).gstPercentApplied()).isEqualTo(5.0);
    }

    @Test
    void restaurant_luxury_applies18Percent() {
        TaxCalculateResponse result = orchestrator.calculate(
                restaurantRequest(Map.of("luxuryCategory", "true")));
        assertThat(result.lines().get(0).gstPercentApplied()).isEqualTo(18.0);
    }

    @Test
    void export_customer_hasZeroTax() {
        TaxCalculateResponse result = orchestrator.calculate(new TaxCalculateRequest(
                true, "29", "29", null, null, 0.0, null,
                List.of(new TaxLineRequest(1, 1L, null, 1.0, 1000.0, 18.0, 0.0,
                        null, null, null, null, null, null, null, null)),
                "RETAIL", "EXPORT", "EXCLUSIVE", LocalDate.now(), null, true, false,
                Map.of()));
        assertThat(result.gstEnabled()).isFalse();
        assertThat(result.taxAmount()).isZero();
    }

    @Test
    void composition_customer_rejected() {
        assertThatThrownBy(() -> orchestrator.calculate(new TaxCalculateRequest(
                true, "29", "29", null, null, 0.0, null,
                List.of(new TaxLineRequest(1, 1L, null, 1.0, 100.0, 18.0, 0.0,
                        null, null, null, null, null, null, null, null)),
                "RETAIL", "COMPOSITION", "INCLUSIVE", LocalDate.now(), null, true, false,
                Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mixedSupply_inclusiveAndExclusiveLines() {
        TaxCalculateResponse result = orchestrator.calculate(new TaxCalculateRequest(
                true, "29", "29", null, null, 0.0, null,
                List.of(
                        line(1, 118.0, 18.0, true),
                        line(2, 100.0, 18.0, false)),
                "RETAIL", "REGISTERED", "INCLUSIVE", LocalDate.now(), null, true, false,
                Map.of()));
        assertThat(result.lines()).hasSize(2);
        assertThat(result.totalAmount()).isGreaterThan(0);
    }

    private TaxCalculateRequest restaurantRequest(Map<String, String> attrs) {
        return new TaxCalculateRequest(
                true, "29", "29", null, null, 0.0, null,
                List.of(new TaxLineRequest(1, 1L, null, 1.0, 105.0, null, 0.0,
                        null, null, null, null, null, null, true, null)),
                "RESTAURANT", "UNREGISTERED", "INCLUSIVE", LocalDate.now(), null, true, false,
                attrs);
    }

    private TaxLineRequest line(int lineNo, double unitPrice, Double gstPercent, boolean inclusive) {
        return new TaxLineRequest(
                lineNo, 1L, null, 1.0, unitPrice, gstPercent, 0.0,
                null, null, null, null, null, null, inclusive, null);
    }
}
