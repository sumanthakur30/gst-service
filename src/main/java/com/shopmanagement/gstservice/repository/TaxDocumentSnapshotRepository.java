package com.shopmanagement.gstservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.DocumentType;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;

public interface TaxDocumentSnapshotRepository extends JpaRepository<TaxDocumentSnapshot, Long> {

    @EntityGraph(attributePaths = "lines", type = EntityGraph.EntityGraphType.LOAD)
    Optional<TaxDocumentSnapshot> findByTenantIdAndId(Long tenantId, Long id);

    Optional<TaxDocumentSnapshot> findByTenantIdAndSourceServiceAndSourceTypeAndSourceIdAndDocumentType(
            Long tenantId, String sourceService, String sourceType, String sourceId, DocumentType documentType);

    /** Unbounded — used by GSTR / compliance period aggregates. Prefer Pageable overload for UI lists. */
    @EntityGraph(attributePaths = "lines", type = EntityGraph.EntityGraphType.LOAD)
    List<TaxDocumentSnapshot> findByTenantIdAndDocumentDateBetweenOrderByDocumentDateDescIdDesc(
            Long tenantId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "lines", type = EntityGraph.EntityGraphType.LOAD)
    List<TaxDocumentSnapshot> findByTenantIdAndDocumentDateBetweenOrderByDocumentDateDescIdDesc(
            Long tenantId, LocalDate from, LocalDate to, Pageable pageable);
}
