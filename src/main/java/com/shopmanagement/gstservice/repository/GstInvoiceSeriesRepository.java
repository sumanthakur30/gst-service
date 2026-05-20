package com.shopmanagement.gstservice.repository;

import java.util.Optional;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.GstInvoiceSeries;

public interface GstInvoiceSeriesRepository extends JpaRepository<GstInvoiceSeries, Long> {

    Optional<GstInvoiceSeries> findByTenantIdAndGstRegistrationIdAndFinancialYearAndSeriesPrefix(
            long tenantId, long gstRegistrationId, String financialYear, String seriesPrefix);
}
