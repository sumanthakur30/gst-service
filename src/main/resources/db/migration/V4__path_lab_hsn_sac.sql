-- Pathology / diagnostic lab SAC codes (India GST — Chapter 99 human health services)
-- Idempotent for re-runs on shared DBs.

INSERT INTO hsn_sac_master (code, description, type, chapter)
SELECT '999316', 'Medical test services (pathology / diagnostics)', 'SAC', '99'
WHERE NOT EXISTS (SELECT 1 FROM hsn_sac_master WHERE code = '999316');

INSERT INTO hsn_sac_master (code, description, type, chapter)
SELECT '999312', 'Medical and dental services', 'SAC', '99'
WHERE NOT EXISTS (SELECT 1 FROM hsn_sac_master WHERE code = '999312');

INSERT INTO hsn_sac_master (code, description, type, chapter)
SELECT '998419', 'Other human health services', 'SAC', '99'
WHERE NOT EXISTS (SELECT 1 FROM hsn_sac_master WHERE code = '998419');

-- 18% GST on diagnostic services (exempt/zero-rated cases configured separately in tax rules)
INSERT INTO gst_rate_schedule (hsn_sac_id, gst_rate_percent, effective_from)
SELECT id, 18.000, DATE '2020-01-01'
FROM hsn_sac_master
WHERE code IN ('999316', '999312', '998419')
  AND NOT EXISTS (
    SELECT 1 FROM gst_rate_schedule g WHERE g.hsn_sac_id = hsn_sac_master.id
  );
