package com.shopmanagement.gstservice.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shopmanagement.gstservice.model.HsnSacMaster;

public interface GstRateLookupRepository extends JpaRepository<HsnSacMaster, Long> {

    @Query(value = """
            SELECT r.gst_rate_percent
            FROM gst_rate_schedule r
            JOIN hsn_sac_master h ON h.id = r.hsn_sac_id
            WHERE h.code = :hsnCode AND h.active = true
              AND r.effective_from <= :onDate
              AND (r.effective_to IS NULL OR r.effective_to >= :onDate)
            ORDER BY r.effective_from DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Double> findRateByHsn(@Param("hsnCode") String hsnCode, @Param("onDate") LocalDate onDate);
}
