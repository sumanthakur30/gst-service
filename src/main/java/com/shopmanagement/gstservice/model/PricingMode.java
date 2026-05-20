package com.shopmanagement.gstservice.model;

public enum PricingMode {
    INCLUSIVE,
    EXCLUSIVE;

    public static PricingMode from(String value) {
        if (value == null || value.isBlank()) {
            return INCLUSIVE;
        }
        try {
            return PricingMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return INCLUSIVE;
        }
    }
}
