package com.shopmanagement.gstservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.EwayBillRequest;

public interface EwayBillRequestRepository extends JpaRepository<EwayBillRequest, Long> {
    Optional<EwayBillRequest> findByTenantIdAndTaxDocumentSnapshotId(Long tenantId, Long taxDocumentSnapshotId);

    Optional<EwayBillRequest> findByIdAndTenantId(Long id, Long tenantId);
}
