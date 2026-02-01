-- Standardize DECIMAL precision for money/financial columns to DECIMAL(15,2).
-- Only widens precision (10,2 -> 15,2 and 12,2 -> 15,2). Never reduces precision.
-- Columns at DECIMAL(19,2) are left as-is to avoid data loss.
-- Rate/percentage columns (DECIMAL(5,2)), score columns, ad metrics, and
-- exchange rate columns are left unchanged.
-- Uses DO blocks to safely skip tables that may not exist in all environments.

-- =============================================================================
-- trendyol_orders: DECIMAL(10,2) -> DECIMAL(15,2)
-- =============================================================================
ALTER TABLE trendyol_orders
    ALTER COLUMN gross_amount TYPE DECIMAL(15,2),
    ALTER COLUMN total_discount TYPE DECIMAL(15,2),
    ALTER COLUMN total_ty_discount TYPE DECIMAL(15,2),
    ALTER COLUMN stoppage TYPE DECIMAL(15,2),
    ALTER COLUMN estimated_commission TYPE DECIMAL(15,2),
    ALTER COLUMN coupon_discount TYPE DECIMAL(15,2),
    ALTER COLUMN early_payment_fee TYPE DECIMAL(15,2),
    ALTER COLUMN estimated_shipping_cost TYPE DECIMAL(15,2);

-- =============================================================================
-- trendyol_products: DECIMAL(10,2) -> DECIMAL(15,2) (money columns only)
-- =============================================================================
ALTER TABLE trendyol_products
    ALTER COLUMN sale_price TYPE DECIMAL(15,2),
    ALTER COLUMN last_shipping_cost_per_unit TYPE DECIMAL(15,2);

-- =============================================================================
-- return_records: DECIMAL(10,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'return_records') THEN
        ALTER TABLE return_records
            ALTER COLUMN product_cost TYPE DECIMAL(15,2),
            ALTER COLUMN shipping_cost_out TYPE DECIMAL(15,2),
            ALTER COLUMN shipping_cost_return TYPE DECIMAL(15,2),
            ALTER COLUMN commission_loss TYPE DECIMAL(15,2),
            ALTER COLUMN packaging_cost TYPE DECIMAL(15,2),
            ALTER COLUMN total_loss TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- store_expenses: DECIMAL(10,2) -> DECIMAL(15,2)
-- =============================================================================
ALTER TABLE store_expenses
    ALTER COLUMN amount TYPE DECIMAL(15,2),
    ALTER COLUMN vat_amount TYPE DECIMAL(15,2),
    ALTER COLUMN net_amount TYPE DECIMAL(15,2);

-- =============================================================================
-- trendyol_cargo_invoices: DECIMAL(10,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trendyol_cargo_invoices') THEN
        ALTER TABLE trendyol_cargo_invoices
            ALTER COLUMN amount TYPE DECIMAL(15,2),
            ALTER COLUMN vat_amount TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- invoices: DECIMAL(10,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'invoices') THEN
        ALTER TABLE invoices
            ALTER COLUMN subtotal TYPE DECIMAL(15,2),
            ALTER COLUMN tax_amount TYPE DECIMAL(15,2),
            ALTER COLUMN total_amount TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- billing_transactions: DECIMAL(10,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'billing_transactions') THEN
        ALTER TABLE billing_transactions
            ALTER COLUMN amount TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- subscription_prices: DECIMAL(10,2) -> DECIMAL(15,2) (money column only)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'subscription_prices') THEN
        ALTER TABLE subscription_prices
            ALTER COLUMN price_amount TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- buybox_tracked_products: DECIMAL(10,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'buybox_tracked_products') THEN
        ALTER TABLE buybox_tracked_products
            ALTER COLUMN alert_price_threshold TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- buybox_snapshots: DECIMAL(10,2) -> DECIMAL(15,2) (price columns only)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'buybox_snapshots') THEN
        ALTER TABLE buybox_snapshots
            ALTER COLUMN winner_price TYPE DECIMAL(15,2),
            ALTER COLUMN my_price TYPE DECIMAL(15,2),
            ALTER COLUMN price_difference TYPE DECIMAL(15,2),
            ALTER COLUMN lowest_price TYPE DECIMAL(15,2),
            ALTER COLUMN highest_price TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- buybox_alerts: DECIMAL(10,2) -> DECIMAL(15,2) (price columns only)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'buybox_alerts') THEN
        ALTER TABLE buybox_alerts
            ALTER COLUMN price_before TYPE DECIMAL(15,2),
            ALTER COLUMN price_after TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- purchase_orders: DECIMAL(12,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'purchase_orders') THEN
        ALTER TABLE purchase_orders
            ALTER COLUMN transportation_cost TYPE DECIMAL(15,2),
            ALTER COLUMN total_cost TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- purchase_order_items: DECIMAL(10,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'purchase_order_items') THEN
        ALTER TABLE purchase_order_items DROP COLUMN IF EXISTS total_cost_per_unit;
        ALTER TABLE purchase_order_items
            ALTER COLUMN manufacturing_cost_per_unit TYPE DECIMAL(15,2),
            ALTER COLUMN transportation_cost_per_unit TYPE DECIMAL(15,2),
            ALTER COLUMN manufacturing_cost_supplier_currency TYPE DECIMAL(15,2);
        ALTER TABLE purchase_order_items
            ADD COLUMN total_cost_per_unit DECIMAL(15,2) GENERATED ALWAYS AS (manufacturing_cost_per_unit + COALESCE(transportation_cost_per_unit, 0)) STORED;
    END IF;
END $$;

-- =============================================================================
-- commission_reconciliation_logs: DECIMAL(12,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'commission_reconciliation_logs') THEN
        ALTER TABLE commission_reconciliation_logs
            ALTER COLUMN total_estimated TYPE DECIMAL(15,2),
            ALTER COLUMN total_real TYPE DECIMAL(15,2),
            ALTER COLUMN total_difference TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- financial_settlements: commission_difference DECIMAL(12,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'financial_settlements') THEN
        ALTER TABLE financial_settlements
            ALTER COLUMN commission_difference TYPE DECIMAL(15,2);
    END IF;
END $$;

-- =============================================================================
-- trendyol_deduction_invoices: NUMERIC(12,2) -> DECIMAL(15,2)
-- =============================================================================
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trendyol_deduction_invoices') THEN
        ALTER TABLE trendyol_deduction_invoices
            ALTER COLUMN debt TYPE DECIMAL(15,2),
            ALTER COLUMN credit TYPE DECIMAL(15,2);
    END IF;
END $$;
