package com.shopmanagement.gstservice.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.TaxCalculateRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxCalculateResponse;
import com.shopmanagement.gstservice.service.TaxCalculationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gst/tax")
@Tag(name = "GST Tax Calculation")
public class TaxCalculationController {

    private final TaxCalculationService taxCalculationService;

    public TaxCalculationController(TaxCalculationService taxCalculationService) {
        this.taxCalculationService = taxCalculationService;
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate invoice tax (9-step enterprise pipeline)")
    public TaxCalculateResponse calculate(@Valid @RequestBody TaxCalculateRequest request) {
        return taxCalculationService.calculate(request);
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview tax before save (no snapshot)")
    public TaxCalculateResponse preview(@Valid @RequestBody TaxCalculateRequest request) {
        return taxCalculationService.calculate(request);
    }

    @PostMapping("/breakdown")
    @Operation(summary = "Alias for calculate — returns CGST/SGST/IGST breakup")
    public TaxCalculateResponse breakdown(@Valid @RequestBody TaxCalculateRequest request) {
        return taxCalculationService.calculate(request);
    }
}
