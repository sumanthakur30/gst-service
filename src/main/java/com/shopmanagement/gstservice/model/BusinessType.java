package com.shopmanagement.gstservice.model;

public enum BusinessType {
    RETAIL,
    RESTAURANT,
    MEDICAL,
    DISTRIBUTOR,
    MANUFACTURING,
    ECOMMERCE,
    POS,
    DEFAULT;

    public static BusinessType from(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        try {
            return BusinessType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DEFAULT;
        }
    }
}
