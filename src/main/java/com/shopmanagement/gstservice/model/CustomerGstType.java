package com.shopmanagement.gstservice.model;

public enum CustomerGstType {
    REGISTERED,
    UNREGISTERED,
    COMPOSITION,
    SEZ,
    EXPORT;

    public static CustomerGstType from(String value) {
        if (value == null || value.isBlank()) {
            return UNREGISTERED;
        }
        try {
            return CustomerGstType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNREGISTERED;
        }
    }
}
