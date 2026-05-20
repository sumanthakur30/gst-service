package com.shopmanagement.gstservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.GstRegistration;

public interface GstRegistrationRepository extends JpaRepository<GstRegistration, Long> {

    List<GstRegistration> findByTenantIdAndActiveTrueOrderByLegalNameAsc(Long tenantId);

    Optional<GstRegistration> findByTenantIdAndId(Long tenantId, Long id);

    Optional<GstRegistration> findByTenantIdAndGstinIgnoreCase(Long tenantId, String gstin);

    boolean existsByTenantIdAndGstinIgnoreCase(Long tenantId, String gstin);
}
