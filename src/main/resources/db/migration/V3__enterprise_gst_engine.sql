-- Enterprise GST engine: masters, profiles, rules, audit (tenant-scoped where applicable)

CREATE TABLE state_code_master (
    code VARCHAR(2) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    union_territory BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE gst_slab_master (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    slab_code VARCHAR(30) NOT NULL,
    gst_rate_percent NUMERIC(6, 3) NOT NULL,
    cess_percent NUMERIC(6, 3) NOT NULL DEFAULT 0,
    description VARCHAR(200),
    supply_nature VARCHAR(30) NOT NULL DEFAULT 'TAXABLE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gst_slab_tenant_code UNIQUE (tenant_id, slab_code)
);

CREATE INDEX idx_gst_slab_tenant_active ON gst_slab_master (tenant_id, active) WHERE deleted_at IS NULL;

CREATE TABLE gst_tax_category (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    category_code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    supply_type VARCHAR(20) NOT NULL DEFAULT 'GOODS',
    default_hsn_sac_id BIGINT REFERENCES hsn_sac_master (id),
    reverse_charge_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tax_cat_tenant_code UNIQUE (tenant_id, category_code)
);

CREATE TABLE tax_master (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tax_code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    tax_type VARCHAR(20) NOT NULL DEFAULT 'GST',
    gst_slab_id BIGINT REFERENCES gst_slab_master (id),
    hsn_sac_id BIGINT REFERENCES hsn_sac_master (id),
    cess_percent NUMERIC(6, 3) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tax_master_tenant_code UNIQUE (tenant_id, tax_code)
);

CREATE INDEX idx_tax_master_tenant ON tax_master (tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE customer_tax_profile (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    customer_ref VARCHAR(64) NOT NULL,
    gstin VARCHAR(15),
    state_code VARCHAR(2),
    customer_gst_type VARCHAR(30) NOT NULL DEFAULT 'UNREGISTERED',
    composition_dealer BOOLEAN NOT NULL DEFAULT FALSE,
    sez_unit BOOLEAN NOT NULL DEFAULT FALSE,
    export_customer BOOLEAN NOT NULL DEFAULT FALSE,
    reverse_charge_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    tcs_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_customer_tax_tenant_ref UNIQUE (tenant_id, customer_ref)
);

CREATE TABLE supplier_tax_profile (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    supplier_ref VARCHAR(64) NOT NULL,
    gstin VARCHAR(15),
    state_code VARCHAR(2),
    supplier_gst_type VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    reverse_charge_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_supplier_tax_tenant_ref UNIQUE (tenant_id, supplier_ref)
);

CREATE TABLE tax_rule_engine (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    rule_code VARCHAR(60) NOT NULL,
    business_type VARCHAR(30),
    priority INT NOT NULL DEFAULT 100,
    condition_json TEXT NOT NULL,
    action_json TEXT NOT NULL,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tax_rule_tenant_code UNIQUE (tenant_id, rule_code)
);

CREATE INDEX idx_tax_rule_lookup ON tax_rule_engine (tenant_id, business_type, active, priority);

CREATE TABLE invoice_tax_details (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tax_document_snapshot_id BIGINT NOT NULL REFERENCES tax_document_snapshot (id),
    business_type VARCHAR(30),
    pricing_mode VARCHAR(20) NOT NULL DEFAULT 'INCLUSIVE',
    customer_gst_type VARCHAR(30),
    cess_total NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tcs_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tds_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    reverse_charge BOOLEAN NOT NULL DEFAULT FALSE,
    e_invoice_json TEXT,
    eway_bill_json TEXT,
    gstr_bucket_json TEXT,
    calculation_version VARCHAR(20) NOT NULL DEFAULT 'v2',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoice_tax_snap ON invoice_tax_details (tax_document_snapshot_id);

CREATE TABLE invoice_item_tax (
    id BIGSERIAL PRIMARY KEY,
    tax_line_snapshot_id BIGINT NOT NULL REFERENCES tax_line_snapshot (id) ON DELETE CASCADE,
    tax_category_code VARCHAR(40),
    supply_nature VARCHAR(30) NOT NULL DEFAULT 'TAXABLE',
    cgst_rate NUMERIC(6, 3) NOT NULL DEFAULT 0,
    sgst_rate NUMERIC(6, 3) NOT NULL DEFAULT 0,
    igst_rate NUMERIC(6, 3) NOT NULL DEFAULT 0,
    cess_rate NUMERIC(6, 3) NOT NULL DEFAULT 0,
    scheme_discount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    free_quantity NUMERIC(14, 3) NOT NULL DEFAULT 0,
    mrp NUMERIC(14, 2),
    batch_ref VARCHAR(64),
    rule_applied VARCHAR(60)
);

CREATE INDEX idx_invoice_item_tax_line ON invoice_item_tax (tax_line_snapshot_id);

CREATE TABLE gst_transaction_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    request_id VARCHAR(64),
    operation VARCHAR(40) NOT NULL,
    source_service VARCHAR(40),
    source_ref VARCHAR(64),
    request_hash VARCHAR(64),
    request_json TEXT,
    response_json TEXT,
    calculation_ms INT,
    created_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gst_tx_log_tenant_time ON gst_transaction_log (tenant_id, created_at DESC);
CREATE INDEX idx_gst_tx_log_request_hash ON gst_transaction_log (tenant_id, request_hash);

-- Extend snapshot header for enterprise fields
ALTER TABLE tax_document_snapshot ADD COLUMN IF NOT EXISTS cess NUMERIC(14, 2) NOT NULL DEFAULT 0;
ALTER TABLE tax_document_snapshot ADD COLUMN IF NOT EXISTS business_type VARCHAR(30);
ALTER TABLE tax_document_snapshot ADD COLUMN IF NOT EXISTS calculation_snapshot_json TEXT;
ALTER TABLE tax_document_snapshot ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Indian states / UTs (GST state codes)
INSERT INTO state_code_master (code, name, union_territory) VALUES
    ('01', 'Jammu and Kashmir', FALSE),
    ('02', 'Himachal Pradesh', FALSE),
    ('03', 'Punjab', FALSE),
    ('04', 'Chandigarh', TRUE),
    ('05', 'Uttarakhand', FALSE),
    ('06', 'Haryana', FALSE),
    ('07', 'Delhi', FALSE),
    ('08', 'Rajasthan', FALSE),
    ('09', 'Uttar Pradesh', FALSE),
    ('10', 'Bihar', FALSE),
    ('11', 'Sikkim', FALSE),
    ('12', 'Arunachal Pradesh', FALSE),
    ('13', 'Nagaland', FALSE),
    ('14', 'Manipur', FALSE),
    ('15', 'Mizoram', FALSE),
    ('16', 'Tripura', FALSE),
    ('17', 'Meghalaya', FALSE),
    ('18', 'Assam', FALSE),
    ('19', 'West Bengal', FALSE),
    ('20', 'Jharkhand', FALSE),
    ('21', 'Odisha', FALSE),
    ('22', 'Chhattisgarh', FALSE),
    ('23', 'Madhya Pradesh', FALSE),
    ('24', 'Gujarat', FALSE),
    ('26', 'Dadra and Nagar Haveli and Daman and Diu', TRUE),
    ('27', 'Maharashtra', FALSE),
    ('29', 'Karnataka', FALSE),
    ('30', 'Goa', FALSE),
    ('31', 'Lakshadweep', TRUE),
    ('32', 'Kerala', FALSE),
    ('33', 'Tamil Nadu', FALSE),
    ('34', 'Puducherry', TRUE),
    ('35', 'Andaman and Nicobar Islands', TRUE),
    ('36', 'Telangana', FALSE),
    ('37', 'Andhra Pradesh', FALSE),
    ('38', 'Ladakh', FALSE),
    ('97', 'Other Territory', TRUE),
    ('99', 'Centre Jurisdiction', FALSE)
ON CONFLICT (code) DO NOTHING;

-- Standard GST slabs (global tenant_id NULL = platform default)
INSERT INTO gst_slab_master (tenant_id, slab_code, gst_rate_percent, description, supply_nature) VALUES
    (NULL, 'NIL', 0.000, 'Nil rated', 'NIL_RATED'),
    (NULL, 'EXEMPT', 0.000, 'Exempt supply', 'EXEMPT'),
    (NULL, 'GST_0', 0.000, 'Zero rated', 'ZERO_RATED'),
    (NULL, 'GST_5', 5.000, '5% GST', 'TAXABLE'),
    (NULL, 'GST_12', 12.000, '12% GST', 'TAXABLE'),
    (NULL, 'GST_18', 18.000, '18% GST', 'TAXABLE'),
    (NULL, 'GST_28', 28.000, '28% GST', 'TAXABLE'),
    (NULL, 'RESTAURANT_5', 5.000, 'Restaurant without ITC (Notification)', 'TAXABLE'),
    (NULL, 'RESTAURANT_18', 18.000, 'Restaurant AC / luxury', 'TAXABLE');

-- Restaurant dine-in vs takeaway rule seeds (platform-wide)
INSERT INTO tax_rule_engine (tenant_id, rule_code, business_type, priority, condition_json, action_json) VALUES
    (NULL, 'RESTAURANT_DINEIN_5', 'RESTAURANT', 10,
     '{"serviceMode":"DINE_IN","luxuryCategory":false}',
     '{"gstSlabCode":"RESTAURANT_5","itcBlocked":true}'),
    (NULL, 'RESTAURANT_TAKEAWAY_5', 'RESTAURANT', 20,
     '{"serviceMode":"TAKEAWAY","luxuryCategory":false}',
     '{"gstSlabCode":"RESTAURANT_5","itcBlocked":true}'),
    (NULL, 'RESTAURANT_AC_18', 'RESTAURANT', 5,
     '{"luxuryCategory":true}',
     '{"gstSlabCode":"RESTAURANT_18","itcBlocked":false}');
