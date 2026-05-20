package com.shopmanagement.gstservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shopmanagement.gstservice.model.HsnSacMaster;

public interface HsnSacMasterRepository extends JpaRepository<HsnSacMaster, Long> {

    Optional<HsnSacMaster> findByCodeIgnoreCase(String code);

    @Query("""
            SELECT h FROM HsnSacMaster h WHERE h.active = true
            AND (LOWER(h.code) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(h.description) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY h.code
            """)
    List<HsnSacMaster> search(@Param("q") String q);
}
