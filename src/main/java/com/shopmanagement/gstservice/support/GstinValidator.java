package com.shopmanagement.gstservice.support;

import java.util.Set;
import java.util.regex.Pattern;

import com.shopmanagement.gstservice.api.GstApi.GstinValidateResponse;

public final class GstinValidator {

    private static final Pattern GST_NUMBER_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private static final Set<String> VALID_STATE_CODES = Set.of(
            "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "26", "27", "29", "30", "31", "32",
            "33", "34", "35", "36", "37", "38", "39", "40", "41", "42",
            "43", "44", "45", "46", "47", "48", "49", "50", "51", "52",
            "53", "54", "55", "56", "57", "58", "59", "60", "61", "62",
            "63", "64", "65", "66", "67", "68", "69", "70", "71", "72",
            "73", "74", "75", "76", "77", "78", "79", "80", "81", "82",
            "83", "84", "85", "86", "87", "88", "89", "90", "91", "92",
            "93", "94", "95", "96", "97", "98", "99");

    private GstinValidator() {
    }

    public static GstinValidateResponse validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return new GstinValidateResponse(false, null, null, "GSTIN is required");
        }
        String normalized = raw.trim().toUpperCase();
        if (!GST_NUMBER_PATTERN.matcher(normalized).matches()) {
            return new GstinValidateResponse(false, normalized, null, "Invalid GSTIN format");
        }
        String stateCode = normalized.substring(0, 2);
        if (!VALID_STATE_CODES.contains(stateCode)) {
            return new GstinValidateResponse(false, normalized, stateCode, "Invalid GST state code");
        }
        return new GstinValidateResponse(true, normalized, stateCode, "Valid GSTIN format");
    }
}
