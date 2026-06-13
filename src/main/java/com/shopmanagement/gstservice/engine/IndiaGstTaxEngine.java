package com.shopmanagement.gstservice.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.shopmanagement.gstservice.api.GstApi.TaxCalculateRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxCalculateResponse;
import com.shopmanagement.gstservice.api.GstApi.TaxLineRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxLineResult;

/**
 * India GST calculation — aligned with order-service {@code GstTaxCalculator} + line-inclusive logic.
 */
@Component
public class IndiaGstTaxEngine {

    private final double defaultGstRatePercent;
    private final double inclusiveFallbackOnMissingLinePercent;

    public IndiaGstTaxEngine(
            @Value("${gst.tax.default-rate-percent:18}") double defaultGstRatePercent,
            @Value("${gst.tax.inclusive-fallback-on-missing-line-percent:18}") double inclusiveFallbackOnMissingLinePercent) {
        this.defaultGstRatePercent = defaultGstRatePercent;
        this.inclusiveFallbackOnMissingLinePercent = inclusiveFallbackOnMissingLinePercent;
    }

    public TaxCalculateResponse calculate(TaxCalculateRequest request) {
        double discount = safe(request.headerDiscount());
        double incomingTax = safe(request.incomingTaxAmount());
        boolean applyGst = request.applyGst();

        List<TaxLineRequest> lines = request.lines() == null ? List.of() : request.lines();
        if (lines.isEmpty()) {
            TaxBreakdown breakdown = calculateHeader(0.0, discount, incomingTax, applyGst,
                    request.sellerStateCode(), request.customerStateCode());
            return toResponse(breakdown, List.of());
        }

        if (!applyGst) {
            double subtotal = sumLineGross(lines);
            TaxBreakdown breakdown = calculateHeader(subtotal, discount, incomingTax, false,
                    request.sellerStateCode(), request.customerStateCode());
            List<TaxLineResult> lineResults = allocateExclusiveProportional(lines, breakdown.taxAmount(), breakdown.taxableAmount());
            return toResponse(breakdown, lineResults);
        }

        double inclusiveTaxableSum = 0.0;
        double inclusiveTaxSum = 0.0;
        double exclusiveSubtotal = 0.0;
        List<TaxLineResult> inclusiveLines = new ArrayList<>();

        double fallbackInclusivePct = inclusiveFallbackOnMissingLinePercent > 0.0
                ? inclusiveFallbackOnMissingLinePercent
                : 0.0;

        for (TaxLineRequest line : lines) {
            double qty = safe(line.quantity());
            double unitPrice = safe(line.unitPrice());
            double lineGross = round2(unitPrice * qty);
            Double rawGstPct = line.gstPercent();
            Double gstPct = rawGstPct;
            if ((gstPct == null || gstPct <= 0.0) && fallbackInclusivePct > 0.0) {
                gstPct = fallbackInclusivePct;
            }
            if (gstPct != null && gstPct > 0) {
                double unitBase = baseFromInclusiveUnitPrice(unitPrice, gstPct);
                double lineTaxable = round2(unitBase * qty);
                double lineTax = round2(lineGross - lineTaxable);
                inclusiveTaxableSum += lineTaxable;
                inclusiveTaxSum += lineTax;
                inclusiveLines.add(lineResult(line.lineNo(), gstPct, lineTaxable, lineTax, lineGross, true, line.hsnSac()));
            } else {
                exclusiveSubtotal += lineGross;
                inclusiveLines.add(lineResult(line.lineNo(), null, lineGross, 0.0, lineGross, false, line.hsnSac()));
            }
        }

        double exclusiveDiscount = inclusiveTaxSum > 0 ? 0.0 : discount;
        TaxBreakdown exclusiveBreakdown = calculateHeader(exclusiveSubtotal, exclusiveDiscount, null, true,
                request.sellerStateCode(), request.customerStateCode());
        double exclusiveTax = exclusiveSubtotal <= 0 ? 0.0 : exclusiveBreakdown.taxAmount();

        double totalTax = round2(inclusiveTaxSum + exclusiveTax);
        double combinedSubtotal = round2(inclusiveTaxableSum + exclusiveSubtotal);

        TaxBreakdown breakdown = calculateInclusiveHeader(
                combinedSubtotal,
                discount,
                totalTax,
                Math.max(0.0, round2(inclusiveTaxableSum + inclusiveTaxSum + exclusiveSubtotal + exclusiveTax - discount)),
                request.sellerStateCode(),
                request.customerStateCode());

        List<TaxLineResult> finalLines = mergeExclusiveLineTax(inclusiveLines, lines, exclusiveTax);
        return toResponse(breakdown, finalLines);
    }

    private List<TaxLineResult> mergeExclusiveLineTax(
            List<TaxLineResult> partial,
            List<TaxLineRequest> sourceLines,
            double exclusiveTax) {
        List<TaxLineResult> exclusiveTargets = partial.stream()
                .filter(l -> !l.taxInclusive())
                .toList();
        if (exclusiveTargets.isEmpty()) {
            return partial;
        }
        double subtotal = exclusiveTargets.stream().mapToDouble(TaxLineResult::taxableValue).sum();
        double remainingTax = exclusiveTax;
        List<TaxLineResult> out = new ArrayList<>();
        int exIdx = 0;
        for (int i = 0; i < partial.size(); i++) {
            TaxLineResult row = partial.get(i);
            if (row.taxInclusive()) {
                out.add(row);
                continue;
            }
            double lineBase = row.taxableValue();
            double itemTax;
            if (exIdx == exclusiveTargets.size() - 1 || subtotal <= 0) {
                itemTax = remainingTax;
            } else {
                itemTax = round2((lineBase / subtotal) * exclusiveTax);
                remainingTax = round2(remainingTax - itemTax);
            }
            exIdx++;
            out.add(lineResult(
                    row.lineNo(),
                    row.gstPercentApplied(),
                    lineBase,
                    itemTax,
                    round2(lineBase + itemTax),
                    false,
                    null));
        }
        return out;
    }

    private List<TaxLineResult> allocateExclusiveProportional(
            List<TaxLineRequest> lines,
            double totalTax,
            double taxable) {
        double subtotal = sumLineGross(lines);
        double remainingTax = totalTax;
        List<TaxLineResult> results = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            TaxLineRequest line = lines.get(i);
            double lineBase = safe(line.unitPrice()) * safe(line.quantity());
            double itemTax;
            if (i == lines.size() - 1 || subtotal <= 0) {
                itemTax = remainingTax;
            } else {
                itemTax = round2((lineBase / subtotal) * totalTax);
                remainingTax = round2(remainingTax - itemTax);
            }
            results.add(lineResult(line.lineNo(), line.gstPercent(), lineBase, itemTax, round2(lineBase + itemTax), false, line.hsnSac()));
        }
        return results;
    }

    private TaxBreakdown calculateHeader(
            double subtotalAmount,
            double discountAmount,
            Double taxAmountInput,
            boolean gstEnabled,
            String shopStateCode,
            String customerStateCode) {
        BigDecimal subtotal = amount(subtotalAmount);
        BigDecimal discount = amount(discountAmount);
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        if (!gstEnabled) {
            return new TaxBreakdown(false, false, round2Bd(subtotal), round2Bd(discount), round2Bd(taxable),
                    0.0, 0.0, 0.0, 0.0, round2Bd(taxable));
        }

        BigDecimal taxAmount = taxAmountInput == null ? BigDecimal.ZERO : amount(taxAmountInput);
        if (taxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            taxAmount = taxable.multiply(BigDecimal.valueOf(defaultGstRatePercent))
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        }
        taxAmount = taxAmount.max(BigDecimal.ZERO);

        boolean interState = customerStateCode != null
                && !customerStateCode.isBlank()
                && shopStateCode != null
                && !shopStateCode.isBlank()
                && !customerStateCode.trim().equals(shopStateCode.trim());

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        if (interState) {
            igst = taxAmount;
        } else {
            cgst = taxAmount.divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            sgst = taxAmount.subtract(cgst);
        }
        BigDecimal total = taxable.add(taxAmount);
        return new TaxBreakdown(
                true,
                interState,
                round2Bd(subtotal),
                round2Bd(discount),
                round2Bd(taxable),
                round2Bd(taxAmount),
                round2Bd(cgst),
                round2Bd(sgst),
                round2Bd(igst),
                round2Bd(total));
    }

    private TaxBreakdown calculateInclusiveHeader(
            double subtotalAmount,
            double discountAmount,
            double taxAmount,
            double totalAmount,
            String shopStateCode,
            String customerStateCode) {
        BigDecimal subtotal = amount(subtotalAmount);
        BigDecimal discount = amount(discountAmount);
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal tax = amount(taxAmount).max(BigDecimal.ZERO);

        boolean interState = customerStateCode != null
                && !customerStateCode.isBlank()
                && shopStateCode != null
                && !shopStateCode.isBlank()
                && !customerStateCode.trim().equals(shopStateCode.trim());

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        if (interState) {
            igst = tax;
        } else {
            cgst = tax.divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            sgst = tax.subtract(cgst);
        }

        return new TaxBreakdown(
                true,
                interState,
                round2Bd(subtotal),
                round2Bd(discount),
                round2Bd(taxable),
                round2Bd(tax),
                round2Bd(cgst),
                round2Bd(sgst),
                round2Bd(igst),
                round2Bd(amount(totalAmount)));
    }

    public static double baseFromInclusiveUnitPrice(double inclusiveUnitPrice, double gstRatePercent) {
        if (gstRatePercent <= 0) {
            return round2Static(inclusiveUnitPrice);
        }
        BigDecimal inc = BigDecimal.valueOf(inclusiveUnitPrice);
        BigDecimal divisor = BigDecimal.ONE.add(
                BigDecimal.valueOf(gstRatePercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return inc.divide(divisor, 2, RoundingMode.HALF_UP).doubleValue();
    }

    private double sumLineGross(List<TaxLineRequest> lines) {
        return lines.stream().mapToDouble(l -> safe(l.unitPrice()) * safe(l.quantity())).sum();
    }

    private TaxCalculateResponse toResponse(TaxBreakdown b, List<TaxLineResult> lines) {
        return new TaxCalculateResponse(
                b.gstEnabled(),
                b.interState(),
                b.subtotalAmount(),
                b.discountAmount(),
                b.taxableAmount(),
                b.taxAmount(),
                b.cgstAmount(),
                b.sgstAmount(),
                b.igstAmount(),
                b.totalAmount(),
                lines,
                0.0,
                0.0,
                null,
                List.of());
    }

    private TaxLineResult lineResult(
            int lineNo,
            Double gstPercent,
            double taxable,
            double tax,
            double total,
            boolean inclusive,
            String hsnSac) {
        return new TaxLineResult(lineNo, gstPercent, taxable, tax, total, inclusive, 0.0, null, null, hsnSac);
    }

    private BigDecimal amount(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round2(double value) {
        return round2Static(value);
    }

    private double round2Bd(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round2Static(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record TaxBreakdown(
            boolean gstEnabled,
            boolean interState,
            double subtotalAmount,
            double discountAmount,
            double taxableAmount,
            double taxAmount,
            double cgstAmount,
            double sgstAmount,
            double igstAmount,
            double totalAmount) {
    }
}
