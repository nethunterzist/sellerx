-- Normalize store status columns to uppercase to match Java enum constant names.
-- Previously, webhookStatus and syncStatus used lowercase values ("pending", "active", "failed", "inactive").
-- After converting these fields from String to Java enums with @Enumerated(EnumType.STRING),
-- all values must match the uppercase enum constant names exactly.

-- Normalize webhook_status: pending -> PENDING, active -> ACTIVE, failed -> FAILED, inactive -> INACTIVE
UPDATE stores SET webhook_status = UPPER(webhook_status)
WHERE webhook_status IS NOT NULL
  AND webhook_status <> UPPER(webhook_status);

-- Normalize sync_status: pending -> PENDING (other values were already uppercase)
UPDATE stores SET sync_status = UPPER(sync_status)
WHERE sync_status IS NOT NULL
  AND sync_status <> UPPER(sync_status);

-- historical_sync_status and overall_sync_status were already uppercase in existing code, but normalize just in case
UPDATE stores SET historical_sync_status = UPPER(historical_sync_status)
WHERE historical_sync_status IS NOT NULL
  AND historical_sync_status <> UPPER(historical_sync_status);

UPDATE stores SET overall_sync_status = UPPER(overall_sync_status)
WHERE overall_sync_status IS NOT NULL
  AND overall_sync_status <> UPPER(overall_sync_status);
