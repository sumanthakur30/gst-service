package com.shopmanagement.gstservice.service;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanagement.gstservice.api.GstApi.InvoiceNumberResponse;
import com.shopmanagement.gstservice.exception.NotFoundException;
import com.shopmanagement.gstservice.model.GstInvoiceSeries;
import com.shopmanagement.gstservice.repository.GstInvoiceSeriesRepository;
import com.shopmanagement.gstservice.repository.GstRegistrationRepository;
import com.shopmanagement.gstservice.support.IndianFinancialYear;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class InvoiceSeriesService {

    private final GstInvoiceSeriesRepository seriesRepository;
    private final GstRegistrationRepository registrationRepository;

    public InvoiceSeriesService(
            GstInvoiceSeriesRepository seriesRepository,
            GstRegistrationRepository registrationRepository) {
        this.seriesRepository = seriesRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public InvoiceNumberResponse nextNumber(long gstRegistrationId, LocalDate documentDate, String seriesPrefix) {
        long tenantId = TenantIds.require();
        registrationRepository.findByTenantIdAndId(tenantId, gstRegistrationId)
                .orElseThrow(() -> new NotFoundException("GST registration not found"));

        String prefix = seriesPrefix == null || seriesPrefix.isBlank() ? "INV" : seriesPrefix.trim();
        String fy = IndianFinancialYear.label(documentDate);

        GstInvoiceSeries series = seriesRepository
                .findByTenantIdAndGstRegistrationIdAndFinancialYearAndSeriesPrefix(
                        tenantId, gstRegistrationId, fy, prefix)
                .orElseGet(() -> createSeries(tenantId, gstRegistrationId, fy, prefix));

        long nextSeq = series.getLastSequence() + 1;
        series.setLastSequence(nextSeq);
        series.setUpdatedAt(Instant.now());
        seriesRepository.save(series);

        String invoiceNumber = prefix + "/" + fy + "/" + String.format("%06d", nextSeq);
        return new InvoiceNumberResponse(fy, invoiceNumber, nextSeq);
    }

    private GstInvoiceSeries createSeries(long tenantId, long gstRegistrationId, String fy, String prefix) {
        GstInvoiceSeries series = new GstInvoiceSeries();
        series.setTenantId(tenantId);
        series.setGstRegistrationId(gstRegistrationId);
        series.setFinancialYear(fy);
        series.setSeriesPrefix(prefix);
        series.setLastSequence(0);
        return seriesRepository.save(series);
    }
}
