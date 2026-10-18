package com.shopmanagement.gstservice.engine.supply;

import java.time.LocalDate;
import java.util.List;

public interface SupplyRuleCatalog {

    SupplyRuleView find(String ruleCode, LocalDate on);

    static SupplyRuleCatalog builtIn() {
        return new BuiltInSupplyRuleCatalog();
    }

    record SupplyRuleView(
            String ruleCode,
            String legalReference,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String outcomeStatus,
            boolean activated) {
    }
}

final class BuiltInSupplyRuleCatalog implements SupplyRuleCatalog {

    static final List<SupplyRuleView> RULES = List.of(
            rule("CIRC92_A_FREE_SAMPLE",
                    "CGST Act s.7(1)(a); Circular No. 92/11/2019-GST dated 07.03.2019 para A",
                    LocalDate.of(2019, 3, 7), null, SupplyDecision.DETERMINED, true),
            rule("SEC_17_5_H_ITC",
                    "CGST Act s.17(5)(h); Circular No. 92/11/2019-GST dated 07.03.2019 para A(ii)",
                    LocalDate.of(2017, 7, 1), null, SupplyDecision.DETERMINED, true),
            rule("CIRC92_B_BUY_X_GET_Y",
                    "CGST Act s.7 and s.8; Circular No. 92/11/2019-GST dated 07.03.2019 para B",
                    LocalDate.of(2019, 3, 7), null, SupplyDecision.DETERMINED, true),
            rule("SEC_8_MIXED",
                    "CGST Act s.8 mixed supply; Circular No. 92/11/2019-GST dated 07.03.2019 para B(ii)",
                    LocalDate.of(2017, 7, 1), null, SupplyDecision.DETERMINED, true),
            rule("SEC_15_3_A_INVOICE_DISCOUNT",
                    "CGST Act s.15(3)(a); Circular No. 92/11/2019-GST dated 07.03.2019 para C",
                    LocalDate.of(2017, 7, 1), null, SupplyDecision.DETERMINED, true),
            rule("SEC_15_3_B_POST_SALE_UNTIL_2025_09_30",
                    "CGST Act s.15(3)(b) as in force until 30.09.2025 (prior agreement, invoice link, recipient ITC reversal)",
                    LocalDate.of(2017, 7, 1), LocalDate.of(2025, 9, 30), SupplyDecision.DETERMINED, true),
            rule("SEC_15_3_B_POST_SALE_FROM_2025_10_01",
                    "Finance Act 2025 GST amendments notified by Notification No. 16/2025-Central Tax dated 17.09.2025, effective 01.10.2025. Published summaries disagree on whether s.15(3)(b) prior-agreement and recipient-ITC conditions were replaced by a s.34 credit note. Do not reduce taxable value until an authorised GST user activates the confirmed text.",
                    LocalDate.of(2025, 10, 1), null, SupplyDecision.REVIEW, false),
            rule("SCHEDULE_I_RELATED_OR_DISTINCT",
                    "CGST Act s.7(1)(c), Schedule I para 2, s.25(4) and s.25(5); CGST Rule 28; Circular No. 210/04/2024-GST",
                    LocalDate.of(2017, 7, 1), null, SupplyDecision.DETERMINED, true),
            rule("SCHEDULE_I_EMPLOYEE_GIFT",
                    "CGST Act Schedule I para 2 proviso (gifts to an employee up to Rs 50,000 in a financial year). The statute does not define the valuation base for the cap.",
                    LocalDate.of(2017, 7, 1), null, SupplyDecision.DETERMINED, true),
            rule("SAME_GSTIN_MOVEMENT",
                    "CGST Act s.25(4): locations under one registration are not distinct persons. Rule 55 delivery challan for movement other than by way of supply.",
                    LocalDate.of(2017, 7, 1), null, SupplyDecision.DETERMINED, true),
            rule("MR_CUSTODY",
                    "Temporary custody is not a supply under CGST Act s.7. ITC under s.17(5)(h) is tested when the goods are disposed of, not when custody changes.",
                    LocalDate.of(2017, 7, 1), null, SupplyDecision.DETERMINED, true),
            rule("IGST_S10_GOODS",
                    "IGST Act s.10 place of supply of goods (location where movement terminates)",
                    LocalDate.of(2017, 7, 1), null, SupplyDecision.DETERMINED, true));

    @Override
    public SupplyRuleView find(String ruleCode, LocalDate on) {
        LocalDate date = on == null ? LocalDate.now() : on;
        return RULES.stream()
                .filter(rule -> rule.ruleCode().equals(ruleCode))
                .filter(rule -> !date.isBefore(rule.effectiveFrom()))
                .filter(rule -> rule.effectiveTo() == null || !date.isAfter(rule.effectiveTo()))
                .findFirst()
                .orElse(null);
    }

    private static SupplyRuleView rule(
            String code,
            String legal,
            LocalDate from,
            LocalDate to,
            String status,
            boolean activated) {
        return new SupplyRuleView(code, legal, from, to, status, activated);
    }
}
