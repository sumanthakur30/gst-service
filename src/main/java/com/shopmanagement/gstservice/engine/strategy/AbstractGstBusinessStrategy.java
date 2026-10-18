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

    /**
     * Buy X get Y taxes the charged paid quantity. A higher-rate free product is a mixed supply.
     */
    protected void applyPromotionalQuantity(GstLineContext line) {
        if (line.freeQuantity() == null || line.freeQuantity().signum() <= 0
                || line.quantity() == null || line.quantity().signum() <= 0) {
            return;
        }
        line.setRuleApplied("BUY_X_GET_Y");
        if (!line.compositeSupply()
                && line.freeProductGstPercent() != null
                && line.gstPercent() != null
                && line.freeProductGstPercent() > line.gstPercent()) {
            line.setGstPercent(line.freeProductGstPercent());
            line.setRuleApplied("SEC_8_MIXED_SUPPLY");
        }
    }
}
