package com.shopmanagement.gstservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.api.GstApi.GstrFilingUploadResponse;
import com.shopmanagement.gstservice.api.GstApi.GstrSummaryRequest;
import com.shopmanagement.gstservice.compliance.GspClientProperties;

class GstFilingUploadServiceTest {

    @Test
    void refusesUploadWhenGspBaseUrlBlank() {
        GspClientProperties props = new GspClientProperties();
        props.setBaseUrl("  ");
        GstFilingUploadService service = new GstFilingUploadService(null, props, new ObjectMapper());
        GstrFilingUploadResponse res = service.upload(new GstrSummaryRequest(LocalDate.now(), LocalDate.now(), null));
        assertFalse(res.accepted());
        assertEquals("NOT_CONFIGURED", res.status());
        assertNull(res.arn());
    }
}
