package com.shopmanagement.gstservice.engine.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.engine.IndiaGstTaxEngine;
import com.shopmanagement.gstservice.engine.context.GstCalculationContext;
import com.shopmanagement.gstservice.engine.context.GstLineContext;
import com.shopmanagement.gstservice.engine.factory.GstStrategyFactory;
import com.shopmanagement.gstservice.engine.strategy.GstBusinessStrategy;
import com.shopmanagement.gstservice.engine.support.GstMoney;
import com.shopmanagement.gstservice.engine.support.GstTaxSplit;
import com.shopmanagement.gstservice.model.CustomerGstType;
import com.shopmanagement.gstservice.model.PricingMode;
import com.shopmanagement.gstservice.model.SupplyNature;

/**
 * Industry-standard 9-step GST invoice calculation sequence.
 */
@Component
public class GstInvoiceCalculationPipeline {

    private final GstStrategyFactory strategyFactory;
    private final double defaultGstRatePercent;

    public GstInvoiceCalculationPipeline(
            GstStrategyFactory strategyFactory,
            @Value("${gst.tax.default-rate-percent:18}") double defaultGstRatePercent) {
        this.strategyFactory = strategyFactory;
        this.defaultGstRatePercent = defaultGstRatePercent;
    }

    public void execute(GstCalculationContext context) {
        GstBusinessStrategy strategy = strategyFactory.resolve(context.businessType());
        validateCustomerRestrictions(context);

        if (!context.applyGst() || isExportWithoutGst(context)) {
            calculateNoGst(context);
            strategy.applyInvoiceRules(context);
            return;
        }

        context.setGstEnabled(true);
        context.setInterState(context.isInterStateSupply());

        // Steps 1–3 + business rules per line
        for (GstLineContext line : context.lines()) {
            strategy.applyLineRules(context, line);
            step1To3LineAmounts(line);
        }

        // Step 4–6: taxable + tax per line
        BigDecimal lineTaxableSum = BigDecimal.ZERO;
        BigDecimal lineTaxSum = BigDecimal.ZERO;
        BigDecimal lineCessSum = BigDecimal.ZERO;
        BigDecimal lineGrossSum = BigDecimal.ZERO;

        for (GstLineContext line : context.lines()) {
            step4To6LineTax(context, line);
            lineTaxableSum = lineTaxableSum.add(line.taxableValue());
            lineTaxSum = lineTaxSum.add(line.taxAmount());
            lineCessSum = lineCessSum.add(line.cessAmount());
            lineGrossSum = lineGrossSum.add(line.lineGross());
        }

        context.setSubtotal(GstMoney.of(GstMoney.toDouble(lineGrossSum)));
        context.setTaxableAmount(GstMoney.of(GstMoney.toDouble(lineTaxableSum)));

        // Step 7: invoice-level discount
        BigDecimal invoiceDiscount = context.invoiceLevelDiscount().add(context.headerDiscount());
        if (invoiceDiscount.signum() > 0 && context.discountBeforeTax()) {
            context.setTotalDiscount(invoiceDiscount);
            BigDecimal adjustedTaxable = context.taxableAmount().subtract(invoiceDiscount).max(BigDecimal.ZERO);
            context.setTaxableAmount(adjustedTaxable);
            if (context.incomingTaxAmount() == null) {
                lineTaxSum = GstMoney.percentOf(adjustedTaxable, defaultGstRatePercent);
            }
        } else {
            context.setTotalDiscount(invoiceDiscount);
        }

        if (context.incomingTaxAmount() != null && context.incomingTaxAmount().signum() > 0) {
            lineTaxSum = context.incomingTaxAmount();
        }

        context.setTotalTax(lineTaxSum);
        context.setTotalCess(lineCessSum);

        GstTaxSplit split = GstTaxSplit.split(lineTaxSum, lineCessSum, context.interState());
        context.setCgst(split.cgst());
        context.setSgst(split.sgst());
        context.setIgst(split.igst());

        BigDecimal preRoundTotal = context.taxableAmount().add(split.totalTax());
        if (!context.discountBeforeTax() && invoiceDiscount.signum() > 0) {
            preRoundTotal = preRoundTotal.subtract(invoiceDiscount).max(BigDecimal.ZERO);
        }

        // Step 8: round-off to nearest rupee (ERP standard)
        BigDecimal rounded = preRoundTotal.setScale(0, GstMoney.ROUND);
        context.setRoundOff(rounded.subtract(preRoundTotal).setScale(GstMoney.SCALE, GstMoney.ROUND));

        // Step 9: grand total
        context.setGrandTotal(rounded);
        strategy.applyInvoiceRules(context);
    }

    private void step1To3LineAmounts(GstLineContext line) {
        BigDecimal qty = line.quantity() == null ? BigDecimal.ZERO : line.quantity();
        BigDecimal unitPrice = line.unitPrice() == null ? BigDecimal.ZERO : line.unitPrice();
        BigDecimal gross = unitPrice.multiply(qty).setScale(GstMoney.SCALE, GstMoney.ROUND);
        line.setLineGross(gross);

        BigDecimal discount = line.lineDiscount() == null ? BigDecimal.ZERO : line.lineDiscount();
        BigDecimal scheme = line.schemeDiscount() == null ? BigDecimal.ZERO : line.schemeDiscount();
        line.setLineGross(gross.subtract(discount).subtract(scheme).max(BigDecimal.ZERO));
    }

    private void step4To6LineTax(GstCalculationContext context, GstLineContext line) {
        if (line.supplyNature() == SupplyNature.EXEMPT
                || line.supplyNature() == SupplyNature.NIL_RATED
                || line.supplyNature() == SupplyNature.NON_GST) {
            line.setTaxableValue(line.lineGross());
            line.setTaxAmount(BigDecimal.ZERO);
            line.setCessAmount(BigDecimal.ZERO);
            line.setLineTotal(line.lineGross());
            line.setResolvedGstPercent(0.0);
            return;
        }

        double rate = resolveRate(line);
        line.setResolvedGstPercent(rate);

        if (line.taxInclusive() || context.pricingMode() == PricingMode.INCLUSIVE) {
            double baseUnit = IndiaGstTaxEngine.baseFromInclusiveUnitPrice(
                    GstMoney.toDouble(line.unitPrice()), rate);
            BigDecimal taxable = GstMoney.of(baseUnit).multiply(line.quantity()).setScale(GstMoney.SCALE, GstMoney.ROUND);
            BigDecimal tax = line.lineGross().subtract(taxable).max(BigDecimal.ZERO).setScale(GstMoney.SCALE, GstMoney.ROUND);
            line.setTaxableValue(taxable);
            line.setTaxAmount(tax);
        } else {
            line.setTaxableValue(line.lineGross());
            line.setTaxAmount(GstMoney.percentOf(line.lineGross(), rate));
        }

        double cessRate = line.cessPercent() == null ? 0.0 : line.cessPercent();
        line.setCessAmount(GstMoney.percentOf(line.taxableValue(), cessRate));
        line.setLineTotal(line.taxableValue().add(line.taxAmount()).add(line.cessAmount()));
    }

    private double resolveRate(GstLineContext line) {
        if (line.gstPercent() != null && line.gstPercent() > 0) {
            return line.gstPercent();
        }
        return defaultGstRatePercent;
    }

    private void calculateNoGst(GstCalculationContext context) {
        context.setGstEnabled(false);
        context.setInterState(false);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (GstLineContext line : context.lines()) {
            step1To3LineAmounts(line);
            line.setTaxableValue(line.lineGross());
            line.setTaxAmount(BigDecimal.ZERO);
            line.setCessAmount(BigDecimal.ZERO);
            line.setLineTotal(line.lineGross());
            subtotal = subtotal.add(line.lineGross());
        }
        BigDecimal discount = context.headerDiscount().add(context.invoiceLevelDiscount());
        context.setSubtotal(subtotal);
        context.setTotalDiscount(discount);
        context.setTaxableAmount(subtotal.subtract(discount).max(BigDecimal.ZERO));
        context.setTotalTax(BigDecimal.ZERO);
        context.setCgst(BigDecimal.ZERO);
        context.setSgst(BigDecimal.ZERO);
        context.setIgst(BigDecimal.ZERO);
        context.setTotalCess(BigDecimal.ZERO);
        context.setRoundOff(BigDecimal.ZERO);
        context.setGrandTotal(context.taxableAmount());
    }

    private boolean isExportWithoutGst(GstCalculationContext context) {
        return context.customerGstType() == CustomerGstType.EXPORT;
    }

    private void validateCustomerRestrictions(GstCalculationContext context) {
        if (context.customerGstType() == CustomerGstType.COMPOSITION && context.applyGst()) {
            throw new IllegalArgumentException("Composition dealer cannot issue tax invoice with standard GST split");
        }
        if (context.customerGstType() == CustomerGstType.SEZ && context.customerGstin() == null) {
            throw new IllegalArgumentException("SEZ supply requires valid buyer GSTIN");
        }
    }

    public List<String> validationWarnings(GstCalculationContext context) {
        List<String> warnings = new ArrayList<>();
        if (context.interState() && context.cgst().signum() > 0) {
            warnings.add("Inter-state supply must not have CGST/SGST");
        }
        if (!context.interState() && context.igst().signum() > 0) {
            warnings.add("Intra-state supply must not have IGST");
        }
        return warnings;
    }
}
