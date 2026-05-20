package com.shopmanagement.gstservice.engine.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GstMoney {

    public static final int SCALE = 2;
    public static final RoundingMode ROUND = RoundingMode.HALF_UP;

    private GstMoney() {
    }

    public static BigDecimal of(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUND);
    }

    public static BigDecimal ofNullable(Double value) {
        return value == null ? BigDecimal.ZERO.setScale(SCALE, ROUND) : of(value);
    }

    public static double toDouble(BigDecimal value) {
        return value.setScale(SCALE, ROUND).doubleValue();
    }

    public static BigDecimal percentOf(BigDecimal base, double ratePercent) {
        if (ratePercent <= 0 || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, ROUND);
        }
        return base.multiply(BigDecimal.valueOf(ratePercent))
                .divide(BigDecimal.valueOf(100), 6, ROUND)
                .setScale(SCALE, ROUND);
    }

    public static BigDecimal roundOffToRupee(BigDecimal amount) {
        return amount.setScale(0, ROUND).subtract(amount.setScale(SCALE, ROUND));
    }
}
