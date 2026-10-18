package com.shopmanagement.gstservice.engine.strategy;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.model.BusinessType;
import com.shopmanagement.gstservice.model.SupplyNature;
import com.shopmanagement.gstservice.service.GstSlabResolver;

@Component
public class MedicalGstStrategy extends AbstractGstBusinessStrategy {

    public MedicalGstStrategy(GstSlabResolver slabResolver) {
        super(slabResolver);
    }

    @Override
    public BusinessType businessType() {
        return BusinessType.MEDICAL;
    }

    @Override
    public void applyLineRules(GstCalculationContext context, GstLineContext line) {
        applyHsnRate(line, context.transactionDate());
        if ("true".equalsIgnoreCase(attr(context, "scheduleDrugExempt"))) {
            line.setGstPercent(0.0);
            line.setSupplyNature(SupplyNature.EXEMPT);
            line.setRuleApplied("SCHEDULE_DRUG_EXEMPT");
        }
        if (line.freeQuantity() != null && line.freeQuantity().signum() > 0 && line.quantity().signum() > 0) {
            applyPromotionalQuantity(line);
        }
    }
}
