-- P1: e-way Part-B update history
CREATE TABLE IF NOT EXISTS eway_part_b_updates (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    eway_bill_request_id BIGINT NOT NULL REFERENCES eway_bill_request(id) ON DELETE CASCADE,
    ewb_no VARCHAR(40),
    vehicle_no VARCHAR(40),
    from_place VARCHAR(120),
    trans_doc_no VARCHAR(80),
    provider VARCHAR(40),
    provider_status VARCHAR(40),
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_eway_part_b_request
    ON eway_part_b_updates (eway_bill_request_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_eway_request_updated
    ON eway_bill_request (tenant_id, updated_at DESC);
