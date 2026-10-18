-- Effective-dated promotional supply rules and auditable decisions.

CREATE TABLE gst_supply_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(80) NOT NULL,
    legal_reference VARCHAR(800) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    outcome_status VARCHAR(40) NOT NULL,
    activated BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_gst_supply_rule_code ON gst_supply_rule (rule_code, effective_from);

CREATE TABLE gst_supply_decision (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    shop_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id VARCHAR(80) NOT NULL,
    classification VARCHAR(60) NOT NULL,
    rule_code VARCHAR(80) NOT NULL,
    legal_reference VARCHAR(800) NOT NULL,
    effective_from DATE NOT NULL,
    status VARCHAR(40) NOT NULL,
    supply_exists BOOLEAN NOT NULL,
    output_gst_applicable BOOLEAN NOT NULL,
    taxable_value NUMERIC(18, 2) NOT NULL,
    valuation_method VARCHAR(60),
    gst_percent NUMERIC(8, 3) NOT NULL,
    inter_state BOOLEAN NOT NULL,
    cgst NUMERIC(18, 2) NOT NULL,
    sgst NUMERIC(18, 2) NOT NULL,
    igst NUMERIC(18, 2) NOT NULL,
    cess NUMERIC(18, 2) NOT NULL,
    itc_treatment VARCHAR(40) NOT NULL,
    itc_cgst NUMERIC(18, 2) NOT NULL,
    itc_sgst NUMERIC(18, 2) NOT NULL,
    itc_igst NUMERIC(18, 2) NOT NULL,
    itc_cess NUMERIC(18, 2) NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    accounting_treatment VARCHAR(80) NOT NULL,
    books_classification VARCHAR(40) NOT NULL,
    portal_return_ready BOOLEAN NOT NULL DEFAULT FALSE,
    explanation VARCHAR(2000) NOT NULL,
    inputs_json TEXT,
    override_by VARCHAR(120),
    override_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gst_supply_decision_source UNIQUE (tenant_id, shop_id, source_type, source_id)
);

CREATE INDEX idx_gst_supply_decision_tenant ON gst_supply_decision (tenant_id, shop_id, status);

INSERT INTO gst_supply_rule (rule_code, legal_reference, effective_from, effective_to, outcome_status, activated) VALUES
('CIRC92_A_FREE_SAMPLE', 'CGST Act s.7(1)(a); Circular No. 92/11/2019-GST dated 07.03.2019 para A', DATE '2019-03-07', NULL, 'DETERMINED', TRUE),
('SEC_17_5_H_ITC', 'CGST Act s.17(5)(h); Circular No. 92/11/2019-GST dated 07.03.2019 para A(ii)', DATE '2017-07-01', NULL, 'DETERMINED', TRUE),
('CIRC92_B_BUY_X_GET_Y', 'CGST Act s.7 and s.8; Circular No. 92/11/2019-GST dated 07.03.2019 para B', DATE '2019-03-07', NULL, 'DETERMINED', TRUE),
('SEC_8_MIXED', 'CGST Act s.8 mixed supply; Circular No. 92/11/2019-GST dated 07.03.2019 para B(ii)', DATE '2017-07-01', NULL, 'DETERMINED', TRUE),
('SEC_15_3_A_INVOICE_DISCOUNT', 'CGST Act s.15(3)(a); Circular No. 92/11/2019-GST dated 07.03.2019 para C', DATE '2017-07-01', NULL, 'DETERMINED', TRUE),
('SEC_15_3_B_POST_SALE_UNTIL_2025_09_30', 'CGST Act s.15(3)(b) as in force until 30.09.2025', DATE '2017-07-01', DATE '2025-09-30', 'DETERMINED', TRUE),
('SEC_15_3_B_POST_SALE_FROM_2025_10_01', 'Finance Act 2025 notified by Notification No. 16/2025-Central Tax dated 17.09.2025. Statutory text of s.15(3)(b) from 01.10.2025 is not activated until authorised GST review.', DATE '2025-10-01', NULL, 'REQUIRES_AUTHORISED_REVIEW', FALSE),
('SCHEDULE_I_RELATED_OR_DISTINCT', 'CGST Act Schedule I para 2, s.25(4), s.25(5); CGST Rule 28; Circular No. 210/04/2024-GST', DATE '2017-07-01', NULL, 'DETERMINED', TRUE),
('SCHEDULE_I_EMPLOYEE_GIFT', 'CGST Act Schedule I para 2 proviso. Rs 50,000 employee gift cap. Valuation base is not defined.', DATE '2017-07-01', NULL, 'DETERMINED', TRUE),
('SAME_GSTIN_MOVEMENT', 'CGST Act s.25(4); CGST Rule 55 delivery challan', DATE '2017-07-01', NULL, 'DETERMINED', TRUE),
('MR_CUSTODY', 'Temporary custody is not a supply under CGST Act s.7', DATE '2017-07-01', NULL, 'DETERMINED', TRUE),
('IGST_S10_GOODS', 'IGST Act s.10 place of supply of goods', DATE '2017-07-01', NULL, 'DETERMINED', TRUE);
