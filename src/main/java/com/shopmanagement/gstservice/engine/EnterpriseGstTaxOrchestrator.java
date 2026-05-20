package com.shopmanagement.gstservice.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.api.GstApi.TaxCalculateRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxCalculateResponse;
import com.shopmanagement.gstservice.api.GstApi.TaxLineRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxLineResult;
import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.engine.pipeline.GstInvoiceCalculationPipeline;
import com.shopmanagement.gstservice.engine.support.GstMoney;
import com.shopmanagement.gstservice.model.BusinessType;
import com.shopmanagement.gstservice.model.CustomerGstType;
import com.shopmanagement.gstservice.model.PricingMode;

@Component
public class EnterpriseGstTaxOrchestrator {

    private final GstInvoiceCalculationPipeline pipeline;
    private final IndiaGstTaxEngine legacyEngine;

    public EnterpriseGstTaxOrchestrator(
            GstInvoiceCalculationPipeline pipeline,
            IndiaGstTaxEngine legacyEngine) {
        this.pipeline = pipeline;
        this.legacyEngine = legacyEngine;
    }

    public TaxCalculateResponse calculate(TaxCalculateRequest request) {
        if (isLegacyRequest(request)) {
            return legacyEngine.calculate(request);
        }
        GstCalculationContext context = toContext(request);
        pipeline.execute(context);
        return toResponse(context);
    }

    private boolean isLegacyRequest(TaxCalculateRequest request) {
        return request.businessType() == null
                && request.pricingMode() == null
                && request.customerGstType() == null
                && (request.businessAttributes() == null || request.businessAttributes().isEmpty())
                && request.invoiceLevelDiscount() == null
                && !Boolean.TRUE.equals(request.reverseCharge());
    }

    private GstCalculationContext toContext(TaxCalculateRequest request) {
        GstCalculationContext context = new GstCalculationContext(
                request.applyGst(),
                BusinessType.from(request.businessType()),
                CustomerGstType.from(request.customerGstType()),
                PricingMode.from(request.pricingMode()),
                request.sellerStateCode(),
                request.customerStateCode(),
                request.sellerGstin(),
                request.customerGstin(),
                request.transactionDate(),
                Boolean.TRUE.equals(request.reverseCharge()),
                request.discountBeforeTax() == null || request.discountBeforeTax(),
                request.businessAttributes());

        context.setHeaderDiscount(GstMoney.ofNullable(request.headerDiscount()));
        context.setInvoiceLevelDiscount(GstMoney.ofNullable(request.invoiceLevelDiscount()));
        if (request.incomingTaxAmount() != null) {
            context.setIncomingTaxAmount(GstMoney.of(request.incomingTaxAmount()));
        }

        List<TaxLineRequest> lines = request.lines() == null ? List.of() : request.lines();
        for (TaxLineRequest line : lines) {
            boolean inclusive = line.taxInclusive() != null
                    ? line.taxInclusive()
                    : PricingMode.from(request.pricingMode()) == PricingMode.INCLUSIVE;
            context.addLine(new GstLineContext(
                    line.lineNo(),
                    line.productId(),
                    line.hsnSac(),
                    GstMoney.ofNullable(line.quantity()),
                    GstMoney.ofNullable(line.unitPrice()),
                    GstMoney.ofNullable(line.discountAmount()),
                    GstMoney.ofNullable(line.schemeDiscount()),
                    line.freeQuantity() == null ? BigDecimal.ZERO : GstMoney.of(line.freeQuantity()),
                    line.gstPercent(),
                    line.cessPercent(),
                    line.mrp() == null ? null : GstMoney.of(line.mrp()),
                    line.batchRef(),
                    line.taxCategoryCode(),
                    inclusive,
                    Boolean.TRUE.equals(line.reverseCharge())));
        }
        return context;
    }

    private TaxCalculateResponse toResponse(GstCalculationContext context) {
        List<TaxLineResult> lineResults = new ArrayList<>();
        for (GstLineContext line : context.lines()) {
            lineResults.add(new TaxLineResult(
                    line.lineNo(),
                    line.resolvedGstPercent(),
                    GstMoney.toDouble(line.taxableValue()),
                    GstMoney.toDouble(line.taxAmount()),
                    GstMoney.toDouble(line.lineTotal()),
                    line.taxInclusive(),
                    GstMoney.toDouble(line.cessAmount()),
                    line.supplyNature().name(),
                    line.ruleApplied(),
                    line.hsnSac()));
        }
        return new TaxCalculateResponse(
                context.gstEnabled(),
                context.interState(),
                GstMoney.toDouble(context.subtotal()),
                GstMoney.toDouble(context.totalDiscount()),
                GstMoney.toDouble(context.taxableAmount()),
                GstMoney.toDouble(context.totalTax()),
                GstMoney.toDouble(context.cgst()),
                GstMoney.toDouble(context.sgst()),
                GstMoney.toDouble(context.igst()),
                GstMoney.toDouble(context.grandTotal()),
                lineResults,
                GstMoney.toDouble(context.totalCess()),
                GstMoney.toDouble(context.roundOff()),
                context.businessType().name(),
                pipeline.validationWarnings(context));
    }
}
