package com.shopmanagement.gstservice.engine.support;

import java.math.BigDecimal;

public record GstTaxSplit(
        boolean interState,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        BigDecimal cess) {

    public BigDecimal totalTax() {
        return cgst.add(sgst).add(igst).add(cess);
    }

    public static GstTaxSplit zero() {
        BigDecimal z = GstMoney.of(0);
        return new GstTaxSplit(false, z, z, z, z);
    }

    public static GstTaxSplit split(BigDecimal totalTax, BigDecimal cess, boolean interState) {
        BigDecimal tax = GstMoney.of(GstMoney.toDouble(totalTax));
        BigDecimal c = GstMoney.of(0);
        if (interState) {
            return new GstTaxSplit(true, c, c, tax, cess);
        }
        BigDecimal half = tax.divide(BigDecimal.valueOf(2), 6, GstMoney.ROUND);
        BigDecimal cgst = half.setScale(GstMoney.SCALE, GstMoney.ROUND);
        BigDecimal sgst = tax.subtract(cgst).setScale(GstMoney.SCALE, GstMoney.ROUND);
        return new GstTaxSplit(false, cgst, sgst, c, cess);
    }
}
