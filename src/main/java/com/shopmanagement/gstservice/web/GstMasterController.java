package com.shopmanagement.gstservice.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.GstSlabResponse;
import com.shopmanagement.gstservice.model.StateCodeMaster;
import com.shopmanagement.gstservice.repository.StateCodeMasterRepository;
import com.shopmanagement.gstservice.service.GstSlabResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/gst/masters")
@Tag(name = "GST Masters")
public class GstMasterController {

    private final GstSlabResolver slabResolver;
    private final StateCodeMasterRepository stateRepository;

    public GstMasterController(GstSlabResolver slabResolver, StateCodeMasterRepository stateRepository) {
        this.slabResolver = slabResolver;
        this.stateRepository = stateRepository;
    }

    @GetMapping("/slabs/{slabCode}")
    @Operation(summary = "Fetch GST slab by code (cached)")
    public GstSlabResponse slab(
            @PathVariable String slabCode,
            @RequestParam(required = false) LocalDate onDate) {
        LocalDate date = onDate == null ? LocalDate.now() : onDate;
        var slab = slabResolver.resolveSlab(date, slabCode);
        if (slab == null) {
            return new GstSlabResponse(slabCode, 0, 0, "UNKNOWN");
        }
        return new GstSlabResponse(slab.slabCode(), slab.gstRatePercent(), slab.cessPercent(), slab.supplyNature());
    }

    @GetMapping("/states")
    @Operation(summary = "List Indian GST state codes")
    public List<StateCodeMaster> states() {
        return stateRepository.findAll();
    }
}
