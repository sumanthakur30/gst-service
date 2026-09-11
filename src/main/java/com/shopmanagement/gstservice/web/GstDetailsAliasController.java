package com.shopmanagement.gstservice.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.GstinDetailsResponse;
import com.shopmanagement.gstservice.service.GstinDetailsLookupService;

@RestController
@RequestMapping("/api/v1/gst")
public class GstDetailsAliasController {

    private final GstinDetailsLookupService gstinDetailsLookupService;

    public GstDetailsAliasController(GstinDetailsLookupService gstinDetailsLookupService) {
        this.gstinDetailsLookupService = gstinDetailsLookupService;
    }

    @GetMapping("/details/{gstin}")
    public GstinDetailsResponse details(@PathVariable String gstin) {
        return gstinDetailsLookupService.lookup(gstin);
    }
}
