package com.shopmanagement.gstservice.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.TaxValidationRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxValidationResponse;
import com.shopmanagement.gstservice.service.GstValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gst")
@Tag(name = "GST Validation")
public class GstValidationController {

    private final GstValidationService validationService;

    public GstValidationController(GstValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate GSTIN, state code, HSN, and interstate rules")
    public TaxValidationResponse validate(@Valid @RequestBody TaxValidationRequest request) {
        return validationService.validate(request);
    }
}
