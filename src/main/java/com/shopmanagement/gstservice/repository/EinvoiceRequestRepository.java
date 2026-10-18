package com.shopmanagement.gstservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.EinvoiceRequest;

public interface EinvoiceRequestRepository extends JpaRepository<EinvoiceRequest, Long> {
    Optional<EinvoiceRequest> findByTenantIdAndTaxDocumentSnapshotId(Long tenantId, Long taxDocumentSnapshotId);

    Optional<EinvoiceRequest> findByIdAndTenantId(Long id, Long tenantId);

    List<EinvoiceRequest> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);

    List<EinvoiceRequest> findByTenantIdAndStatusIgnoreCaseOrderByUpdatedAtDesc(Long tenantId, String status);
}
