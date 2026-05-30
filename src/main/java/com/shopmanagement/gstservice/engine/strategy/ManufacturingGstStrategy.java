package com.shopmanagement.gstservice.engine.strategy;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.model.BusinessType;
import com.shopmanagement.gstservice.service.GstSlabResolver;

@Component
public class ManufacturingGstStrategy extends AbstractGstBusinessStrategy {

    public ManufacturingGstStrategy(GstSlabResolver slabResolver) {
        super(slabResolver);
    }

    @Override
    public BusinessType businessType() {
        return BusinessType.MANUFACTURING;
    }

    @Override
    public void applyLineRules(GstCalculationContext context, GstLineContext line) {
        String materialType = attr(context, "materialType");
        if ("RAW_MATERIAL".equalsIgnoreCase(materialType)) {
            line.setRuleApplied("RAW_MATERIAL_ITC");
        } else if ("JOB_WORK".equalsIgnoreCase(materialType)) {
            line.setRuleApplied("JOB_WORK_GST");
        }
        applyHsnRateWhenMissing(context, line);
    }
}
