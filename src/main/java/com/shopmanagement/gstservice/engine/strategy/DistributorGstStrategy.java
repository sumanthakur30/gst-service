package com.shopmanagement.gstservice.engine.strategy;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.model.BusinessType;
import com.shopmanagement.gstservice.service.GstSlabResolver;

@Component
public class DistributorGstStrategy extends AbstractGstBusinessStrategy {

    public DistributorGstStrategy(GstSlabResolver slabResolver) {
        super(slabResolver);
    }

    @Override
    public BusinessType businessType() {
        return BusinessType.DISTRIBUTOR;
    }

    @Override
    public void applyLineRules(GstCalculationContext context, GstLineContext line) {
        if ("true".equalsIgnoreCase(attr(context, "reverseCharge"))) {
            line.setGstPercent(line.gstPercent() == null ? 18.0 : line.gstPercent());
        }
        applyHsnRateWhenMissing(context, line);
        Double conversionFactor = parseDouble(attr(context, "secondaryUnitFactor"));
        if (conversionFactor != null && conversionFactor > 0) {
            line.setQuantity(line.quantity().multiply(java.math.BigDecimal.valueOf(conversionFactor)));
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
