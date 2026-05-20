package com.shopmanagement.gstservice.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanagement.gstservice.api.GstApi.CreditNotePostRequest;
import com.shopmanagement.gstservice.api.GstApi.InvoiceNumberRequest;
import com.shopmanagement.gstservice.api.GstApi.InvoiceNumberResponse;
import com.shopmanagement.gstservice.api.GstApi.TaxDocumentPostRequest;
import com.shopmanagement.gstservice.api.GstApi.TaxDocumentSnapshotResponse;
import com.shopmanagement.gstservice.service.InvoiceSeriesService;
import com.shopmanagement.gstservice.service.TaxDocumentSnapshotService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gst")
public class TaxDocumentController {

    private final TaxDocumentSnapshotService snapshotService;
    private final InvoiceSeriesService invoiceSeriesService;

    public TaxDocumentController(
            TaxDocumentSnapshotService snapshotService,
            InvoiceSeriesService invoiceSeriesService) {
        this.snapshotService = snapshotService;
        this.invoiceSeriesService = invoiceSeriesService;
    }

    @PostMapping("/documents/post")
    public TaxDocumentSnapshotResponse post(@Valid @RequestBody TaxDocumentPostRequest request) {
        return snapshotService.post(request);
    }

    @PostMapping("/documents/credit-note")
    public TaxDocumentSnapshotResponse postCreditNote(@Valid @RequestBody CreditNotePostRequest request) {
        return snapshotService.postCreditNote(request);
    }

    @GetMapping("/documents/{id}")
    public TaxDocumentSnapshotResponse get(@PathVariable Long id) {
        return snapshotService.get(id);
    }

    @GetMapping("/documents")
    public List<TaxDocumentSnapshotResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return snapshotService.list(from, to);
    }

    @PostMapping("/invoices/next-number")
    public InvoiceNumberResponse nextInvoiceNumber(@Valid @RequestBody InvoiceNumberRequest request) {
        return invoiceSeriesService.nextNumber(
                request.gstRegistrationId(),
                request.documentDate(),
                request.seriesPrefix());
    }
}
