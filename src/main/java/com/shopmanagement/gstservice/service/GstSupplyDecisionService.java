package com.shopmanagement.gstservice.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanagement.gstservice.engine.supply.GstSupplyDecisionEngine;
import com.shopmanagement.gstservice.engine.supply.SupplyDecision;
import com.shopmanagement.gstservice.engine.supply.SupplyFacts;
import com.shopmanagement.gstservice.engine.supply.SupplyRuleCatalog;
import com.shopmanagement.gstservice.filter.RequestIdFilter;
import com.shopmanagement.gstservice.model.GstSupplyDecisionRecord;
import com.shopmanagement.gstservice.model.GstSupplyRule;
import com.shopmanagement.gstservice.repository.GstSupplyDecisionRepository;
import com.shopmanagement.gstservice.repository.GstSupplyRuleRepository;

@Service
public class GstSupplyDecisionService {

    private final GstSupplyRuleRepository ruleRepository;
    private final GstSupplyDecisionRepository decisionRepository;
    private final ObjectMapper objectMapper;

    public GstSupplyDecisionService(
            GstSupplyRuleRepository ruleRepository,
            GstSupplyDecisionRepository decisionRepository,
            ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.decisionRepository = decisionRepository;
        this.objectMapper = objectMapper;
    }

    public SupplyDecision preview(SupplyFacts facts) {
        return engine().decide(facts);
    }

    @Transactional
    public GstSupplyDecisionRecord record(
            String shopId,
            String sourceType,
            String sourceId,
            SupplyFacts facts,
            boolean recalculate) {
        Long tenantId = requireTenant();
        GstSupplyDecisionRecord existing = decisionRepository
                .findByTenantIdAndShopIdAndSourceTypeAndSourceId(tenantId, shopId, sourceType, sourceId)
                .orElse(null);
        if (existing != null && !recalculate) {
            return existing;
        }
        if (existing != null && existing.getOverrideBy() != null && !existing.getOverrideBy().isBlank()) {
            return existing;
        }
        SupplyDecision decision = engine().decide(facts);
        GstSupplyDecisionRecord row = existing == null ? new GstSupplyDecisionRecord() : existing;
        row.setTenantId(tenantId);
        row.setShopId(shopId);
        row.setSourceType(sourceType);
        row.setSourceId(sourceId);
        copy(decision, row);
        row.setInputsJson(writeFacts(facts));
        row.setUpdatedAt(Instant.now());
        if (row.getCreatedAt() == null) {
            row.setCreatedAt(Instant.now());
        }
        return decisionRepository.save(row);
    }

    @Transactional
    public GstSupplyRule activatePostSaleRule(String actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("An authorised reason is required to activate the post-supply discount rule");
        }
        GstSupplyRule rule = ruleRepository.findByRuleCode("SEC_15_3_B_POST_SALE_FROM_2025_10_01").stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Post-supply discount rule is not seeded"));
        rule.setActivated(true);
        rule.setOutcomeStatus(SupplyDecision.DETERMINED);
        rule.setLegalReference(rule.getLegalReference()
                + " Activated by " + actor + " on " + LocalDate.now() + ": " + reason.trim());
        return ruleRepository.save(rule);
    }

    public GstSupplyDecisionRecord get(Long id) {
        return decisionRepository.findByTenantIdAndId(requireTenant(), id)
                .orElseThrow(() -> new IllegalArgumentException("Supply decision not found"));
    }

    private GstSupplyDecisionEngine engine() {
        List<GstSupplyRule> rows = ruleRepository.findAll();
        SupplyRuleCatalog catalog = (code, on) -> {
            LocalDate date = on == null ? LocalDate.now() : on;
            return rows.stream()
                    .filter(row -> code.equals(row.getRuleCode()))
                    .filter(row -> !date.isBefore(row.getEffectiveFrom()))
                    .filter(row -> row.getEffectiveTo() == null || !date.isAfter(row.getEffectiveTo()))
                    .findFirst()
                    .map(row -> new SupplyRuleCatalog.SupplyRuleView(
                            row.getRuleCode(),
                            row.getLegalReference(),
                            row.getEffectiveFrom(),
                            row.getEffectiveTo(),
                            row.getOutcomeStatus(),
                            row.isActivated()))
                    .orElseGet(() -> SupplyRuleCatalog.builtIn().find(code, date));
        };
        return new GstSupplyDecisionEngine(catalog);
    }

    private static void copy(SupplyDecision decision, GstSupplyDecisionRecord row) {
        row.setClassification(decision.classification());
        row.setRuleCode(decision.ruleCode());
        row.setLegalReference(trim(decision.legalReference(), 800));
        row.setEffectiveFrom(decision.effectiveFrom());
        row.setStatus(decision.status());
        row.setSupplyExists(decision.supplyExists());
        row.setOutputGstApplicable(decision.outputGstApplicable());
        row.setTaxableValue(decision.taxableValue());
        row.setValuationMethod(decision.valuationMethod());
        row.setGstPercent(java.math.BigDecimal.valueOf(decision.gstPercent()));
        row.setInterState(decision.interState());
        row.setCgst(decision.cgst());
        row.setSgst(decision.sgst());
        row.setIgst(decision.igst());
        row.setCess(decision.cess());
        row.setItcTreatment(decision.itcTreatment());
        row.setItcCgst(decision.itcCgst());
        row.setItcSgst(decision.itcSgst());
        row.setItcIgst(decision.itcIgst());
        row.setItcCess(decision.itcCess());
        row.setDocumentType(decision.documentType());
        row.setAccountingTreatment(decision.accountingTreatment());
        row.setBooksClassification(decision.booksClassification());
        row.setPortalReturnReady(false);
        row.setExplanation(trim(decision.explanation(), 2000));
    }

    private String writeFacts(SupplyFacts facts) {
        try {
            return objectMapper.writeValueAsString(facts);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private static Long requireTenant() {
        Long tenantId = RequestIdFilter.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantId;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
