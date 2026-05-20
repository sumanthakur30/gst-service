-- Phase 1: immutable snapshots linkage, invoice numbering per GSTIN + FY

ALTER TABLE tax_document_snapshot
    ADD COLUMN IF NOT EXISTS original_snapshot_id BIGINT REFERENCES tax_document_snapshot (id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tax_snap_source_doc
    ON tax_document_snapshot (tenant_id, source_service, source_type, source_id, document_type);

CREATE TABLE gst_invoice_series (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    gst_registration_id BIGINT NOT NULL REFERENCES gst_registration (id),
    financial_year VARCHAR(9) NOT NULL,
    series_prefix VARCHAR(20) NOT NULL DEFAULT 'INV',
    last_sequence BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gst_inv_series UNIQUE (tenant_id, gst_registration_id, financial_year, series_prefix)
);

CREATE INDEX idx_gst_inv_series_reg ON gst_invoice_series (gst_registration_id, financial_year);
