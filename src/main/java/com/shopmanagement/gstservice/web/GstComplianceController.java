package com.shopmanagement.gstservice.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.GstrSummaryRequest;
import com.shopmanagement.gstservice.api.GstApi.GstrSummaryResponse;
import com.shopmanagement.gstservice.service.GstComplianceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gst/compliance")
@Tag(name = "GST Compliance")
public class GstComplianceController {

    private final GstComplianceService complianceService;

    public GstComplianceController(GstComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @PostMapping("/gstr-summary")
    @Operation(summary = "Build GSTR-1 / 3B / HSN summary dataset from posted tax snapshots")
    public GstrSummaryResponse gstrSummary(@Valid @RequestBody GstrSummaryRequest request) {
        return complianceService.buildGstrSummary(request);
    }
}
