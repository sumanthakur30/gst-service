package com.shopmanagement.gstservice.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.GstinDetailsResponse;
import com.shopmanagement.gstservice.api.GstApi.GstinValidateRequest;
import com.shopmanagement.gstservice.api.GstApi.GstinValidateResponse;
import com.shopmanagement.gstservice.service.GstinDetailsLookupService;
import com.shopmanagement.gstservice.support.GstinValidator;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gst/gstin")
public class GstinController {

    private final GstinDetailsLookupService gstinDetailsLookupService;

    public GstinController(GstinDetailsLookupService gstinDetailsLookupService) {
        this.gstinDetailsLookupService = gstinDetailsLookupService;
    }

    @PostMapping("/validate")
    public GstinValidateResponse validate(@Valid @RequestBody GstinValidateRequest request) {
        return GstinValidator.validate(request.gstin());
    }

    @GetMapping("/details/{gstin}")
    public GstinDetailsResponse details(@PathVariable String gstin) {
        return gstinDetailsLookupService.lookup(gstin);
    }
}
