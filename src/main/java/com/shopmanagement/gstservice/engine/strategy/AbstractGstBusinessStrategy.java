package com.shopmanagement.gstservice.engine.strategy;

import java.time.LocalDate;

import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.model.SupplyNature;
import com.shopmanagement.gstservice.service.GstSlabResolver;

public abstract class AbstractGstBusinessStrategy implements GstBusinessStrategy {

    protected final GstSlabResolver slabResolver;

    protected AbstractGstBusinessStrategy(GstSlabResolver slabResolver) {
        this.slabResolver = slabResolver;
    }

    protected void applySlab(GstCalculationContext context, GstLineContext line, String slabCode, String ruleCode) {
        var slab = slabResolver.resolveSlab(context.transactionDate(), slabCode);
        if (slab == null) {
            return;
        }
        line.setGstPercent(slab.gstRatePercent());
        line.setSupplyNature(SupplyNature.fromSlabCode(slab.slabCode()));
        line.setRuleApplied(ruleCode);
    }

    protected String attr(GstCalculationContext context, String key) {
        return context.businessAttributes().get(key);
    }

    protected void applyHsnRate(GstLineContext line, LocalDate onDate) {
        Double rate = slabResolver.resolveRateByHsn(line.hsnSac(), onDate);
        if (rate != null) {
            line.setGstPercent(rate);
        }
    }

    protected void applyHsnRateWhenMissing(GstCalculationContext context, GstLineContext line) {
        if (line.gstPercent() == null || line.gstPercent() <= 0) {
            applyHsnRate(line, context.transactionDate());
        }
    }
}
