package com.shopmanagement.gstservice.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class IndianFinancialYearTest {

    @Test
    void label_aprilStartsNewYear() {
        assertEquals("2025-26", IndianFinancialYear.label(LocalDate.of(2025, 4, 1)));
        assertEquals("2025-26", IndianFinancialYear.label(LocalDate.of(2026, 3, 31)));
        assertEquals("2026-27", IndianFinancialYear.label(LocalDate.of(2026, 4, 1)));
    }
}
