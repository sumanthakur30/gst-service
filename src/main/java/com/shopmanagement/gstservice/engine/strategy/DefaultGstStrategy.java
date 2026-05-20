package com.shopmanagement.gstservice.engine.strategy;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.model.BusinessType;
import com.shopmanagement.gstservice.service.GstSlabResolver;

@Component
public class DefaultGstStrategy extends AbstractGstBusinessStrategy {

    public DefaultGstStrategy(GstSlabResolver slabResolver) {
        super(slabResolver);
    }

    @Override
    public BusinessType businessType() {
        return BusinessType.DEFAULT;
    }

    @Override
    public void applyLineRules(GstCalculationContext context, GstLineContext line) {
        if (line.gstPercent() == null || line.gstPercent() <= 0) {
            slabResolver.resolveRateByHsn(line.hsnSac(), context.transactionDate())
                    .ifPresent(line::setGstPercent);
        }
    }
}
