package com.shopmanagement.gstservice.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanagement.gstservice.api.GstApi.BranchMapResponse;
import com.shopmanagement.gstservice.api.GstApi.BranchMapUpsert;
import com.shopmanagement.gstservice.exception.ConflictException;
import com.shopmanagement.gstservice.exception.NotFoundException;
import com.shopmanagement.gstservice.model.GstRegistration;
import com.shopmanagement.gstservice.model.GstRegistrationBranchMap;
import com.shopmanagement.gstservice.repository.GstRegistrationBranchMapRepository;
import com.shopmanagement.gstservice.repository.GstRegistrationRepository;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class BranchMapService {

    private final GstRegistrationBranchMapRepository mapRepository;
    private final GstRegistrationRepository registrationRepository;

    public BranchMapService(
            GstRegistrationBranchMapRepository mapRepository,
            GstRegistrationRepository registrationRepository) {
        this.mapRepository = mapRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public List<BranchMapResponse> list() {
        long tenantId = TenantIds.require();
        return mapRepository.findByTenantIdOrderByShopIdAsc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchMapResponse getByShop(String shopId) {
        long tenantId = TenantIds.require();
        return mapRepository.findByTenantIdAndShopId(tenantId, shopId.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("No GST registration mapped for shop"));
    }

    @Transactional
    public BranchMapResponse upsert(BranchMapUpsert body) {
        long tenantId = TenantIds.require();
        GstRegistration reg = registrationRepository.findByTenantIdAndId(tenantId, body.gstRegistrationId())
                .orElseThrow(() -> new NotFoundException("GST registration not found"));

        String shopId = body.shopId().trim();
        GstRegistrationBranchMap map = mapRepository.findByTenantIdAndShopId(tenantId, shopId)
                .orElseGet(GstRegistrationBranchMap::new);

        if (map.getId() != null
                && mapRepository.existsByTenantIdAndShopIdAndIdNot(tenantId, shopId, map.getId())) {
            throw new ConflictException("shopId already mapped");
        }

        map.setTenantId(tenantId);
        map.setGstRegistrationId(reg.getId());
        map.setBranchId(body.branchId());
        map.setShopId(shopId);
        map.setDefault(body.isDefault() != null && body.isDefault());
        if (map.getCreatedAt() == null) {
            map.setCreatedAt(Instant.now());
        }
        return toResponse(mapRepository.save(map));
    }

    private BranchMapResponse toResponse(GstRegistrationBranchMap map) {
        String gstin = registrationRepository.findById(map.getGstRegistrationId())
                .map(GstRegistration::getGstin)
                .orElse(null);
        return new BranchMapResponse(
                map.getId(),
                map.getGstRegistrationId(),
                gstin,
                map.getBranchId(),
                map.getShopId(),
                map.isDefault());
    }
}
