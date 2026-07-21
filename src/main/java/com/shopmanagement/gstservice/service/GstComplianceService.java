package com.shopmanagement.gstservice.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.api.GstApi.GstrDocumentRow;
import com.shopmanagement.gstservice.api.GstApi.GstrFilingPackResponse;
import com.shopmanagement.gstservice.api.GstApi.GstrSummaryRequest;
import com.shopmanagement.gstservice.api.GstApi.GstrSummaryResponse;
import com.shopmanagement.gstservice.model.DocumentType;
import com.shopmanagement.gstservice.model.SupplyType;
import com.shopmanagement.gstservice.model.TaxDocumentSnapshot;
import com.shopmanagement.gstservice.model.TaxLineSnapshot;
import com.shopmanagement.gstservice.repository.TaxDocumentSnapshotRepository;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class GstComplianceService {

    private static final String FILING_DISCLAIMER =
            "SugamFlow portal-oriented prep pack from tax snapshots. Validate against GSTN schema "
                    + "before upload; this is not a live GST portal submission.";

    private static final DateTimeFormatter DOC_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final TaxDocumentSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public GstComplianceService(
            TaxDocumentSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GstrSummaryResponse buildGstrSummary(GstrSummaryRequest request) {
        List<TaxDocumentSnapshot> snapshots = loadSnapshots(request);
        double taxable = 0;
        double cgst = 0;
        double sgst = 0;
        double igst = 0;
        double cess = 0;
        List<GstrDocumentRow> documents = new ArrayList<>(snapshots.size());
        Map<String, HsnAgg> hsnByCode = new LinkedHashMap<>();

        for (TaxDocumentSnapshot snap : snapshots) {
            taxable += snap.getTaxableValue().doubleValue();
            cgst += snap.getCgst().doubleValue();
            sgst += snap.getSgst().doubleValue();
            igst += snap.getIgst().doubleValue();
            double snapCess = snap.getCess() != null ? snap.getCess().doubleValue() : 0.0;
            cess += snapCess;
            documents.add(new GstrDocumentRow(
                    snap.getId(),
                    snap.getSourceNumber(),
                    snap.getDocumentDate(),
                    snap.getDocumentType() != null ? snap.getDocumentType().name() : null,
                    snap.getSupplyType() != null ? snap.getSupplyType().name() : null,
                    snap.getBuyerGstin(),
                    snap.getTaxableValue().doubleValue(),
                    snap.getCgst().doubleValue(),
                    snap.getSgst().doubleValue(),
                    snap.getIgst().doubleValue(),
                    snapCess,
                    snap.getGrandTotal() != null ? snap.getGrandTotal().doubleValue() : 0.0));

            accumulateHsn(snap, hsnByCode);
        }

        List<HsnRow> hsnRows = toHsnRows(hsnByCode);
        String gstr1 = toJson(new Gstr1Bucket(snapshots.size(), taxable, cgst, sgst, igst));
        String gstr3b = toJson(new Gstr3bBucket(taxable, cgst + sgst + igst, cess));
        String hsn = toJson(new HsnSummary(hsnRows.size(), hsnRows));

        return new GstrSummaryResponse(
                request.fromDate(),
                request.toDate(),
                round2(taxable),
                round2(cgst),
                round2(sgst),
                round2(igst),
                round2(cess),
                snapshots.size(),
                gstr1,
                gstr3b,
                hsn,
                documents);
    }

    @Transactional(readOnly = true)
    public GstrFilingPackResponse buildGstrFilingPack(GstrSummaryRequest request) {
        List<TaxDocumentSnapshot> snapshots = loadSnapshots(request);
        String returnPeriod = String.format(
                Locale.ROOT, "%02d%04d", request.toDate().getMonthValue(), request.toDate().getYear());

        Map<String, List<Map<String, Object>>> b2bByCtin = new LinkedHashMap<>();
        List<Map<String, Object>> b2cs = new ArrayList<>();
        Map<String, List<Map<String, Object>>> cdnrByCtin = new LinkedHashMap<>();
        Map<String, HsnAgg> hsnByCode = new LinkedHashMap<>();
        int taxInvoiceCount = 0;
        int creditNoteCount = 0;
        int debitNoteCount = 0;

        double taxable = 0;
        double cgst = 0;
        double sgst = 0;
        double igst = 0;
        double cess = 0;

        for (TaxDocumentSnapshot snap : snapshots) {
            taxable += snap.getTaxableValue().doubleValue();
            cgst += snap.getCgst().doubleValue();
            sgst += snap.getSgst().doubleValue();
            igst += snap.getIgst().doubleValue();
            cess += snap.getCess() != null ? snap.getCess().doubleValue() : 0.0;
            accumulateHsn(snap, hsnByCode);

            DocumentType docType = snap.getDocumentType();
            if (docType == DocumentType.CREDIT_NOTE || docType == DocumentType.DEBIT_NOTE) {
                if (docType == DocumentType.CREDIT_NOTE) {
                    creditNoteCount++;
                } else {
                    debitNoteCount++;
                }
                String ctin = normalizeGstin(snap.getBuyerGstin());
                if (ctin == null) {
                    continue;
                }
                cdnrByCtin.computeIfAbsent(ctin, k -> new ArrayList<>()).add(cdnrInvoice(snap, docType));
                continue;
            }

            if (docType == DocumentType.TAX_INVOICE
                    || docType == DocumentType.BILL_OF_SUPPLY
                    || docType == DocumentType.SALES_RECEIPT
                    || docType == null) {
                taxInvoiceCount++;
                if (snap.getSupplyType() == SupplyType.B2B && normalizeGstin(snap.getBuyerGstin()) != null) {
                    String ctin = normalizeGstin(snap.getBuyerGstin());
                    b2bByCtin.computeIfAbsent(ctin, k -> new ArrayList<>()).add(b2bInvoice(snap));
                } else {
                    b2cs.add(b2csRow(snap));
                }
            }
        }

        List<Map<String, Object>> b2b = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : b2bByCtin.entrySet()) {
            Map<String, Object> party = new LinkedHashMap<>();
            party.put("ctin", e.getKey());
            party.put("inv", e.getValue());
            b2b.add(party);
        }

        List<Map<String, Object>> cdnr = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : cdnrByCtin.entrySet()) {
            Map<String, Object> party = new LinkedHashMap<>();
            party.put("ctin", e.getKey());
            party.put("nt", e.getValue());
            cdnr.add(party);
        }

        List<Map<String, Object>> hsnData = new ArrayList<>();
        for (HsnRow row : toHsnRows(hsnByCode)) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("hsn_sc", "UNCLASSIFIED".equals(row.hsnSac()) ? "" : row.hsnSac());
            h.put("desc", "");
            h.put("uqc", "NOS");
            h.put("qty", row.quantity());
            h.put("rt", row.gstRatePercent() != null ? row.gstRatePercent() : 0);
            h.put("txval", row.taxableValue());
            h.put("iamt", row.igst());
            h.put("camt", row.cgst());
            h.put("samt", row.sgst());
            h.put("csamt", row.cess());
            hsnData.add(h);
        }

        Map<String, Object> docIssue = new LinkedHashMap<>();
        List<Map<String, Object>> docDetails = new ArrayList<>();
        docDetails.add(docStat("Invoices for outward supply", taxInvoiceCount));
        docDetails.add(docStat("Credit Note", creditNoteCount));
        docDetails.add(docStat("Debit Note", debitNoteCount));
        docIssue.put("doc_det", docDetails);

        Map<String, Object> gstr1 = new LinkedHashMap<>();
        gstr1.put("gstin", "");
        gstr1.put("fp", returnPeriod);
        gstr1.put("gt", 0);
        gstr1.put("cur_gt", round2(taxable));
        gstr1.put("b2b", b2b);
        gstr1.put("b2cs", b2cs);
        gstr1.put("cdnr", cdnr);
        Map<String, Object> hsn = new LinkedHashMap<>();
        hsn.put("data", hsnData);
        gstr1.put("hsn", hsn);
        gstr1.put("doc_issue", docIssue);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", "sugamflow");
        meta.put("fromDate", request.fromDate().toString());
        meta.put("toDate", request.toDate().toString());
        meta.put("documentCount", snapshots.size());
        meta.put("disclaimer", FILING_DISCLAIMER);
        gstr1.put("meta", meta);

        Map<String, Object> gstr3b = new LinkedHashMap<>();
        gstr3b.put("gstin", "");
        gstr3b.put("ret_period", returnPeriod);
        Map<String, Object> supDetails = new LinkedHashMap<>();
        Map<String, Object> osupDet = new LinkedHashMap<>();
        osupDet.put("txval", round2(taxable));
        osupDet.put("iamt", round2(igst));
        osupDet.put("camt", round2(cgst));
        osupDet.put("samt", round2(sgst));
        osupDet.put("csamt", round2(cess));
        supDetails.put("osup_det", osupDet);
        gstr3b.put("sup_details", supDetails);
        gstr3b.put("meta", meta);

        return new GstrFilingPackResponse(
                request.fromDate(),
                request.toDate(),
                returnPeriod,
                snapshots.size(),
                toJson(gstr1),
                toJson(gstr3b),
                FILING_DISCLAIMER);
    }

    private List<TaxDocumentSnapshot> loadSnapshots(GstrSummaryRequest request) {
        long tenantId = TenantIds.require();
        return snapshotRepository.findByTenantIdAndDocumentDateBetweenOrderByDocumentDateDescIdDesc(
                tenantId, request.fromDate(), request.toDate());
    }

    private static void accumulateHsn(TaxDocumentSnapshot snap, Map<String, HsnAgg> hsnByCode) {
        if (snap.getLines() == null) {
            return;
        }
        for (TaxLineSnapshot line : snap.getLines()) {
            String hsn = normalizeHsn(line.getHsnSac());
            HsnAgg agg = hsnByCode.computeIfAbsent(hsn, HsnAgg::new);
            agg.lineCount += 1;
            agg.quantity += safe(line.getQuantity());
            agg.taxableValue += safe(line.getTaxableValue());
            agg.cgst += safe(line.getCgst());
            agg.sgst += safe(line.getSgst());
            agg.igst += safe(line.getIgst());
            agg.cess += safe(line.getCess());
            if (line.getGstRatePercent() != null) {
                agg.gstRatePercent = line.getGstRatePercent().doubleValue();
            }
        }
    }

    private static List<HsnRow> toHsnRows(Map<String, HsnAgg> hsnByCode) {
        return hsnByCode.values().stream()
                .sorted(Comparator.comparing((HsnAgg a) -> a.taxableValue).reversed())
                .map(a -> new HsnRow(
                        a.hsnSac,
                        a.gstRatePercent,
                        round2(a.quantity),
                        round2(a.taxableValue),
                        round2(a.cgst),
                        round2(a.sgst),
                        round2(a.igst),
                        round2(a.cess),
                        a.lineCount))
                .toList();
    }

    private static Map<String, Object> b2bInvoice(TaxDocumentSnapshot snap) {
        Map<String, Object> inv = new LinkedHashMap<>();
        inv.put("inum", blankToDash(snap.getSourceNumber()));
        inv.put("idt", formatDocDate(snap.getDocumentDate()));
        inv.put("val", round2(safe(snap.getGrandTotal())));
        inv.put("pos", blankToDash(snap.getPlaceOfSupplyState() != null
                ? snap.getPlaceOfSupplyState()
                : snap.getBuyerStateCode()));
        inv.put("rchrg", "N");
        inv.put("inv_typ", "R");
        inv.put("itms", List.of(Map.of("num", 1, "itm_det", itemDet(snap))));
        return inv;
    }

    private static Map<String, Object> b2csRow(TaxDocumentSnapshot snap) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sply_ty", snap.getIgst() != null && snap.getIgst().doubleValue() > 0.009 ? "INTER" : "INTRA");
        row.put("pos", blankToDash(snap.getPlaceOfSupplyState() != null
                ? snap.getPlaceOfSupplyState()
                : snap.getBuyerStateCode()));
        row.put("typ", "OE");
        row.put("rt", inferRate(snap));
        row.put("txval", round2(safe(snap.getTaxableValue())));
        row.put("iamt", round2(safe(snap.getIgst())));
        row.put("camt", round2(safe(snap.getCgst())));
        row.put("samt", round2(safe(snap.getSgst())));
        row.put("csamt", round2(safe(snap.getCess())));
        return row;
    }

    private static Map<String, Object> cdnrInvoice(TaxDocumentSnapshot snap, DocumentType docType) {
        Map<String, Object> nt = new LinkedHashMap<>();
        nt.put("ntty", docType == DocumentType.CREDIT_NOTE ? "C" : "D");
        nt.put("nt_num", blankToDash(snap.getSourceNumber()));
        nt.put("nt_dt", formatDocDate(snap.getDocumentDate()));
        nt.put("val", round2(safe(snap.getGrandTotal())));
        nt.put("pos", blankToDash(snap.getPlaceOfSupplyState() != null
                ? snap.getPlaceOfSupplyState()
                : snap.getBuyerStateCode()));
        nt.put("rchrg", "N");
        nt.put("inv_typ", "R");
        nt.put("itms", List.of(Map.of("num", 1, "itm_det", itemDet(snap))));
        return nt;
    }

    private static Map<String, Object> itemDet(TaxDocumentSnapshot snap) {
        Map<String, Object> det = new LinkedHashMap<>();
        det.put("rt", inferRate(snap));
        det.put("txval", round2(safe(snap.getTaxableValue())));
        det.put("iamt", round2(safe(snap.getIgst())));
        det.put("camt", round2(safe(snap.getCgst())));
        det.put("samt", round2(safe(snap.getSgst())));
        det.put("csamt", round2(safe(snap.getCess())));
        return det;
    }

    private static Map<String, Object> docStat(String docName, int count) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("doc_num", 1);
        row.put("doc_typ", docName);
        row.put("docs", List.of(Map.of(
                "num", 1,
                "from", "1",
                "to", String.valueOf(Math.max(count, 0)),
                "totnum", count,
                "cancel", 0,
                "net_issue", count)));
        return row;
    }

    private static double inferRate(TaxDocumentSnapshot snap) {
        double taxable = safe(snap.getTaxableValue());
        if (taxable < 0.009) {
            return 0;
        }
        double tax = safe(snap.getCgst()) + safe(snap.getSgst()) + safe(snap.getIgst());
        return round2((tax / taxable) * 100.0);
    }

    private static String formatDocDate(LocalDate date) {
        return date != null ? date.format(DOC_DATE) : "";
    }

    private static String normalizeGstin(String gstin) {
        if (gstin == null || gstin.isBlank()) {
            return null;
        }
        String g = gstin.trim().toUpperCase(Locale.ROOT);
        return g.length() >= 15 ? g : null;
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private static String normalizeHsn(String hsn) {
        if (hsn == null || hsn.isBlank()) {
            return "UNCLASSIFIED";
        }
        return hsn.trim().toUpperCase(Locale.ROOT);
    }

    private static double safe(java.math.BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Gstr1Bucket(int documentCount, double taxableValue, double cgst, double sgst, double igst) {
    }

    private record Gstr3bBucket(double taxableValue, double totalTax, double cess) {
    }

    private record HsnSummary(int lineCountHint, List<HsnRow> rows) {
    }

    private record HsnRow(
            String hsnSac,
            Double gstRatePercent,
            double quantity,
            double taxableValue,
            double cgst,
            double sgst,
            double igst,
            double cess,
            int lineCount) {
    }

    private static final class HsnAgg {
        private final String hsnSac;
        private int lineCount;
        private double quantity;
        private double taxableValue;
        private double cgst;
        private double sgst;
        private double igst;
        private double cess;
        private Double gstRatePercent;

        private HsnAgg(String hsnSac) {
            this.hsnSac = hsnSac;
        }
    }
}
