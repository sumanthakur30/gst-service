package com.shopmanagement.gstservice.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.BranchMapResponse;
import com.shopmanagement.gstservice.api.GstApi.BranchMapUpsert;
import com.shopmanagement.gstservice.service.BranchMapService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gst/branches")
public class BranchMapController {

    private final BranchMapService branchMapService;

    public BranchMapController(BranchMapService branchMapService) {
        this.branchMapService = branchMapService;
    }

    @GetMapping("/map")
    public List<BranchMapResponse> list() {
        return branchMapService.list();
    }

    @GetMapping("/map/by-shop")
    public BranchMapResponse getByShop(@RequestParam String shopId) {
        return branchMapService.getByShop(shopId);
    }

    @PostMapping("/map")
    public BranchMapResponse upsert(@Valid @RequestBody BranchMapUpsert body) {
        return branchMapService.upsert(body);
    }
}
