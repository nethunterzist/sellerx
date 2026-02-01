-- V55: Commission Reconciliation System for Hybrid Sync
-- Tracks reconciliation between estimated (Orders API) and real (Settlement API) commissions

-- Reconciliation log table
CREATE TABLE IF NOT EXISTS commission_reconciliation_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_date DATE NOT NULL,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    total_reconciled INTEGER DEFAULT 0,
    total_estimated DECIMAL(12,2) DEFAULT 0,
    total_real DECIMAL(12,2) DEFAULT 0,
    total_difference DECIMAL(12,2) DEFAULT 0,
    average_accuracy DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add commission_difference column to trendyol_orders
-- This tracks the difference between estimated and real commission after reconciliation
ALTER TABLE trendyol_orders
ADD COLUMN IF NOT EXISTS commission_difference DECIMAL(12,2);

-- Index: Fast lookup for orders with estimated commissions (for reconciliation job)
CREATE INDEX IF NOT EXISTS idx_orders_commission_estimated
ON trendyol_orders(is_commission_estimated)
WHERE is_commission_estimated = true;

-- Index: Store-based reconciliation queries
CREATE INDEX IF NOT EXISTS idx_reconciliation_store_date
ON commission_reconciliation_log(store_id, reconciliation_date);

-- Index: Orders by data source for hybrid sync analysis
CREATE INDEX IF NOT EXISTS idx_orders_data_source
ON trendyol_orders(data_source)
WHERE data_source IS NOT NULL;

-- Index: Combined index for reconciliation job (store + estimated flag)
CREATE INDEX IF NOT EXISTS idx_orders_store_estimated
ON trendyol_orders(store_id, is_commission_estimated)
WHERE is_commission_estimated = true;

-- Comment on table
COMMENT ON TABLE commission_reconciliation_log IS 'Tracks daily reconciliation between estimated (Orders API) and real (Settlement API) commissions';

-- Comments on columns
COMMENT ON COLUMN commission_reconciliation_log.total_reconciled IS 'Number of orders reconciled on this date';
COMMENT ON COLUMN commission_reconciliation_log.total_estimated IS 'Sum of estimated commissions before reconciliation';
COMMENT ON COLUMN commission_reconciliation_log.total_real IS 'Sum of real commissions from Settlement API';
COMMENT ON COLUMN commission_reconciliation_log.total_difference IS 'Total difference (real - estimated)';
COMMENT ON COLUMN commission_reconciliation_log.average_accuracy IS 'Percentage accuracy of estimations (0-100)';
COMMENT ON COLUMN trendyol_orders.commission_difference IS 'Difference between estimated and real commission after reconciliation';
