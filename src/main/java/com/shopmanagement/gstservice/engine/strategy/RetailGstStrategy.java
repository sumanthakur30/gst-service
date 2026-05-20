package com.shopmanagement.gstservice.engine.strategy;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.model.BusinessType;
import com.shopmanagement.gstservice.model.PricingMode;
import com.shopmanagement.gstservice.service.GstSlabResolver;

@Component
public class RetailGstStrategy extends AbstractGstBusinessStrategy {

    public RetailGstStrategy(GstSlabResolver slabResolver) {
        super(slabResolver);
    }

    @Override
    public BusinessType businessType() {
        return BusinessType.RETAIL;
    }

    @Override
    public void applyLineRules(GstCalculationContext context, GstLineContext line) {
        if (line.mrp() != null && line.mrp().signum() > 0) {
            line.setUnitPrice(line.mrp());
        }
        if (line.gstPercent() == null || line.gstPercent() <= 0) {
            slabResolver.resolveRateByHsn(line.hsnSac(), context.transactionDate())
                    .ifPresent(rate -> line.setGstPercent(rate));
        }
        line.setTaxInclusive(context.pricingMode() == PricingMode.INCLUSIVE);
    }
}
