package com.shopmanagement.gstservice.service;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.shopmanagement.gstservice.model.GstSlabMaster;
import com.shopmanagement.gstservice.repository.GstRateLookupRepository;
import com.shopmanagement.gstservice.repository.GstSlabMasterRepository;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class GstSlabResolver {

    private final GstSlabMasterRepository slabRepository;
    private final GstRateLookupRepository rateLookupRepository;
    private final double defaultRatePercent;

    public GstSlabResolver(
            GstSlabMasterRepository slabRepository,
            GstRateLookupRepository rateLookupRepository,
            @Value("${gst.tax.default-rate-percent:18}") double defaultRatePercent) {
        this.slabRepository = slabRepository;
        this.rateLookupRepository = rateLookupRepository;
        this.defaultRatePercent = defaultRatePercent;
    }

    public record ResolvedSlab(String slabCode, double gstRatePercent, double cessPercent, String supplyNature) {
    }

    @Cacheable(
            cacheNames = "gstSlabs",
            key = "T(com.shopmanagement.gstservice.support.TenantIds).currentOrNull() + ':' + #slabCode + ':' + #onDate",
            unless = "#result == null")
    public ResolvedSlab resolveSlab(LocalDate onDate, String slabCode) {
        Long tenantId = TenantIds.currentOrNull();
        return slabRepository.findBestEffectiveSlab(tenantId, slabCode, onDate)
                .map(this::toResolved)
                .orElse(null);
    }

    /**
     * Returns GST rate for an HSN/SAC, or {@code null} when code is blank (not cached).
     * Unknown codes fall back to {@code gst.tax.default-rate-percent}.
     */
    @Cacheable(cacheNames = "gstHsnRates", key = "#hsnCode + ':' + #onDate", unless = "#result == null")
    public Double resolveRateByHsn(String hsnCode, LocalDate onDate) {
        if (hsnCode == null || hsnCode.isBlank()) {
            return null;
        }
        return rateLookupRepository.findRateByHsn(hsnCode.trim(), onDate)
                .orElse(defaultRatePercent);
    }

    private ResolvedSlab toResolved(GstSlabMaster slab) {
        return new ResolvedSlab(
                slab.getSlabCode(),
                slab.gstRatePercent(),
                slab.cessPercent(),
                slab.getSupplyNature());
    }
}
