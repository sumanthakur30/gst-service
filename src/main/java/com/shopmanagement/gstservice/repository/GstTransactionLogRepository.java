package com.shopmanagement.gstservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.GstTransactionLog;

public interface GstTransactionLogRepository extends JpaRepository<GstTransactionLog, Long> {
}
