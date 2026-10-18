-- E-Invoice / E-Way request staging (mock provider first; NIC/GSP plugs in later).
CREATE TABLE IF NOT EXISTS einvoice_request (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tax_document_snapshot_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL DEFAULT 'mock',
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    irn VARCHAR(100),
    ack_no VARCHAR(64),
    ack_date TIMESTAMP,
    signed_qr_payload TEXT,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_einvoice_tenant_snapshot
    ON einvoice_request (tenant_id, tax_document_snapshot_id);

CREATE INDEX IF NOT EXISTS idx_einvoice_tenant_status
    ON einvoice_request (tenant_id, status);

CREATE TABLE IF NOT EXISTS eway_bill_request (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tax_document_snapshot_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL DEFAULT 'mock',
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    distance_km INT,
    vehicle_no VARCHAR(40),
    transporter_id VARCHAR(40),
    transporter_name VARCHAR(200),
    ewb_no VARCHAR(40),
    valid_upto TIMESTAMP,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_eway_tenant_snapshot
    ON eway_bill_request (tenant_id, tax_document_snapshot_id);

CREATE INDEX IF NOT EXISTS idx_eway_tenant_status
    ON eway_bill_request (tenant_id, status);
