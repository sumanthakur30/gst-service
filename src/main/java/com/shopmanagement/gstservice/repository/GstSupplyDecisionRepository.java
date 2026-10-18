package com.shopmanagement.gstservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.GstSupplyDecisionRecord;

public interface GstSupplyDecisionRepository extends JpaRepository<GstSupplyDecisionRecord, Long> {

    Optional<GstSupplyDecisionRecord> findByTenantIdAndShopIdAndSourceTypeAndSourceId(
            Long tenantId, String shopId, String sourceType, String sourceId);

    Optional<GstSupplyDecisionRecord> findByTenantIdAndId(Long tenantId, Long id);
}
