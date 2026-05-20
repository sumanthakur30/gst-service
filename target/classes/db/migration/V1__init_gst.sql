-- SugamFlow gst-service — Phase 0 masters + snapshot scaffolding

CREATE TABLE gst_registration (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    legal_name VARCHAR(300) NOT NULL,
    gstin VARCHAR(15) NOT NULL,
    state_code VARCHAR(2) NOT NULL,
    registration_type VARCHAR(30) NOT NULL DEFAULT 'REGULAR',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gst_reg_tenant_gstin UNIQUE (tenant_id, gstin)
);

CREATE INDEX idx_gst_reg_tenant_active ON gst_registration (tenant_id, active);

CREATE TABLE gst_registration_branch_map (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    gst_registration_id BIGINT NOT NULL REFERENCES gst_registration (id),
    branch_id BIGINT,
    shop_id VARCHAR(64),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gst_branch_shop UNIQUE (tenant_id, shop_id)
);

CREATE INDEX idx_gst_branch_reg ON gst_registration_branch_map (gst_registration_id);

CREATE TABLE hsn_sac_master (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL,
    type VARCHAR(10) NOT NULL DEFAULT 'HSN',
    chapter VARCHAR(10),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_hsn_sac_code UNIQUE (code)
);

CREATE TABLE gst_rate_schedule (
    id BIGSERIAL PRIMARY KEY,
    hsn_sac_id BIGINT REFERENCES hsn_sac_master (id),
    tax_category_code VARCHAR(40),
    gst_rate_percent NUMERIC(6, 3) NOT NULL,
    cess_percent NUMERIC(6, 3) NOT NULL DEFAULT 0,
    effective_from DATE NOT NULL,
    effective_to DATE,
    version_no INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gst_rate_hsn_dates ON gst_rate_schedule (hsn_sac_id, effective_from, effective_to);

CREATE TABLE tax_document_snapshot (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    source_service VARCHAR(40) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    source_number VARCHAR(64),
    document_type VARCHAR(40) NOT NULL,
    document_date DATE NOT NULL,
    place_of_supply_state VARCHAR(2),
    seller_gst_registration_id BIGINT REFERENCES gst_registration (id),
    buyer_gstin VARCHAR(15),
    buyer_state_code VARCHAR(2),
    supply_type VARCHAR(20) NOT NULL DEFAULT 'B2C',
    subtotal NUMERIC(14, 2) NOT NULL,
    discount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    taxable_value NUMERIC(14, 2) NOT NULL,
    total_tax NUMERIC(14, 2) NOT NULL,
    cgst NUMERIC(14, 2) NOT NULL DEFAULT 0,
    sgst NUMERIC(14, 2) NOT NULL DEFAULT 0,
    igst NUMERIC(14, 2) NOT NULL DEFAULT 0,
    round_off NUMERIC(14, 2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(14, 2) NOT NULL,
    tax_summary_json TEXT,
    snapshot_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120)
);

CREATE INDEX idx_tax_snap_tenant_date ON tax_document_snapshot (tenant_id, document_date DESC);
CREATE INDEX idx_tax_snap_source ON tax_document_snapshot (source_service, source_type, source_id);

CREATE TABLE tax_line_snapshot (
    id BIGSERIAL PRIMARY KEY,
    tax_document_snapshot_id BIGINT NOT NULL REFERENCES tax_document_snapshot (id) ON DELETE CASCADE,
    line_no INT NOT NULL,
    product_id BIGINT,
    description VARCHAR(300),
    hsn_sac VARCHAR(20),
    quantity NUMERIC(14, 3) NOT NULL,
    unit_price NUMERIC(14, 2) NOT NULL,
    discount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    taxable_value NUMERIC(14, 2) NOT NULL,
    gst_rate_percent NUMERIC(6, 3),
    cgst NUMERIC(14, 2) NOT NULL DEFAULT 0,
    sgst NUMERIC(14, 2) NOT NULL DEFAULT 0,
    igst NUMERIC(14, 2) NOT NULL DEFAULT 0,
    cess NUMERIC(14, 2) NOT NULL DEFAULT 0,
    line_total NUMERIC(14, 2) NOT NULL,
    tax_inclusive BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_tax_line_snap_doc ON tax_line_snapshot (tax_document_snapshot_id);

-- Seed common HSN + 18% rate (extend via admin API later)
INSERT INTO hsn_sac_master (code, description, type, chapter) VALUES
    ('30049099', 'Medicaments (other)', 'HSN', '30'),
    ('21069099', 'Food preparations n.e.c.', 'HSN', '21'),
    ('998314', 'Wholesale and retail services', 'SAC', '99');

INSERT INTO gst_rate_schedule (hsn_sac_id, gst_rate_percent, effective_from)
SELECT id, 18.000, DATE '2020-01-01' FROM hsn_sac_master;
