package com.shopmanagement.gstservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.GstRegistrationBranchMap;

public interface GstRegistrationBranchMapRepository extends JpaRepository<GstRegistrationBranchMap, Long> {

    Optional<GstRegistrationBranchMap> findByTenantIdAndShopId(Long tenantId, String shopId);

    List<GstRegistrationBranchMap> findByTenantIdAndGstRegistrationId(Long tenantId, Long gstRegistrationId);

    List<GstRegistrationBranchMap> findByTenantIdOrderByShopIdAsc(Long tenantId);

    boolean existsByTenantIdAndShopIdAndIdNot(Long tenantId, String shopId, Long id);
}
