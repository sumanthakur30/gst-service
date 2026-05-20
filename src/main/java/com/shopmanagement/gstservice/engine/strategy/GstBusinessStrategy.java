package com.shopmanagement.gstservice.engine.strategy;

import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.model.BusinessType;

public interface GstBusinessStrategy {

    BusinessType businessType();

    /** Resolve GST rate and supply nature before standard pipeline tax step. */
    void applyLineRules(GstCalculationContext context, GstLineContext line);

    /** Optional header-level adjustments after line aggregation. */
    default void applyInvoiceRules(GstCalculationContext context) {
        // no-op
    }
}
