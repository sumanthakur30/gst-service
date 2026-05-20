package com.shopmanagement.gstservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shopmanagement.gstservice.model.GstSlabMaster;

public interface GstSlabMasterRepository extends JpaRepository<GstSlabMaster, Long> {

    @Query("""
            SELECT s FROM GstSlabMaster s
            WHERE s.active = true AND s.deletedAt IS NULL
              AND (s.tenantId IS NULL OR s.tenantId = :tenantId)
              AND s.slabCode = :slabCode
              AND s.effectiveFrom <= :onDate
              AND (s.effectiveTo IS NULL OR s.effectiveTo >= :onDate)
            ORDER BY s.tenantId DESC NULLS LAST
            """)
    List<GstSlabMaster> findEffectiveSlab(
            @Param("tenantId") Long tenantId,
            @Param("slabCode") String slabCode,
            @Param("onDate") LocalDate onDate);

    default Optional<GstSlabMaster> findBestEffectiveSlab(Long tenantId, String slabCode, LocalDate onDate) {
        List<GstSlabMaster> rows = findEffectiveSlab(tenantId, slabCode, onDate);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
