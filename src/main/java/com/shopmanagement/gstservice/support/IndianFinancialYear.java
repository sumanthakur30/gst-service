package com.shopmanagement.gstservice.support;

import java.time.LocalDate;

public final class IndianFinancialYear {

    private IndianFinancialYear() {
    }

    /** e.g. 2025-04-01 → {@code 2025-26}; 2026-01-15 → {@code 2025-26}. */
    public static String label(LocalDate date) {
        int year = date.getYear();
        int startYear = date.getMonthValue() >= 4 ? year : year - 1;
        int endShort = (startYear + 1) % 100;
        return startYear + "-" + String.format("%02d", endShort);
    }
}
