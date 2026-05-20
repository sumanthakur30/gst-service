package com.shopmanagement.gstservice.model;

public enum SupplyNature {
    TAXABLE,
    EXEMPT,
    NIL_RATED,
    ZERO_RATED,
    NON_GST;

    public static SupplyNature fromSlabCode(String slabCode) {
        if (slabCode == null) {
            return TAXABLE;
        }
        return switch (slabCode.toUpperCase()) {
            case "EXEMPT" -> EXEMPT;
            case "NIL" -> NIL_RATED;
            case "GST_0" -> ZERO_RATED;
            default -> TAXABLE;
        };
    }
}
