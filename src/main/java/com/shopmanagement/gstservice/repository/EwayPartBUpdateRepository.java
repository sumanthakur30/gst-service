package com.shopmanagement.gstservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.EwayPartBUpdate;

public interface EwayPartBUpdateRepository extends JpaRepository<EwayPartBUpdate, Long> {
    List<EwayPartBUpdate> findByEwayBillRequestIdOrderByCreatedAtDesc(Long ewayBillRequestId);
}
