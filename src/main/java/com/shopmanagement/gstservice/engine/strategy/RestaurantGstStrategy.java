package com.shopmanagement.gstservice.engine.strategy;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.model.BusinessType;
import com.shopmanagement.gstservice.service.GstSlabResolver;

@Component
public class RestaurantGstStrategy extends AbstractGstBusinessStrategy {

    public RestaurantGstStrategy(GstSlabResolver slabResolver) {
        super(slabResolver);
    }

    @Override
    public BusinessType businessType() {
        return BusinessType.RESTAURANT;
    }

    @Override
    public void applyLineRules(GstCalculationContext context, GstLineContext line) {
        boolean luxury = "true".equalsIgnoreCase(attr(context, "luxuryCategory"))
                || "true".equalsIgnoreCase(attr(context, "acService"));
        if (luxury) {
            applySlab(context, line, "RESTAURANT_18", "RESTAURANT_AC_18");
            return;
        }
        String serviceMode = attr(context, "serviceMode");
        if (serviceMode == null || serviceMode.isBlank()) {
            serviceMode = "DINE_IN";
        }
        applySlab(context, line, "RESTAURANT_5", "RESTAURANT_" + serviceMode + "_5");
    }
}
