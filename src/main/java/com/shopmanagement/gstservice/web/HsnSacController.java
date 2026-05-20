package com.shopmanagement.gstservice.web;

import java.util.List;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.HsnSacResponse;
import com.shopmanagement.gstservice.service.GstSlabResolver;
import com.shopmanagement.gstservice.service.HsnSacService;

@RestController
@RequestMapping("/api/v1/gst/hsn")
public class HsnSacController {

    private final HsnSacService hsnSacService;
    private final GstSlabResolver slabResolver;

    public HsnSacController(HsnSacService hsnSacService, GstSlabResolver slabResolver) {
        this.hsnSacService = hsnSacService;
        this.slabResolver = slabResolver;
    }

    @GetMapping("/search")
    public List<HsnSacResponse> search(@RequestParam String q) {
        return hsnSacService.search(q);
    }

    @GetMapping("/{hsnCode}/rate")
    public HsnRateResponse rateByHsn(
            @PathVariable String hsnCode,
            @RequestParam(required = false) LocalDate onDate) {
        LocalDate date = onDate == null ? LocalDate.now() : onDate;
        double rate = slabResolver.resolveRateByHsn(hsnCode, date).orElse(0.0);
        return new HsnRateResponse(hsnCode, rate);
    }

    public record HsnRateResponse(String hsn, double rate) {
    }
}
