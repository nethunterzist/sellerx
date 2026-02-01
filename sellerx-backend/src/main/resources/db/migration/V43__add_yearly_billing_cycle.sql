-- Add YEARLY billing cycle to subscriptions table constraint
ALTER TABLE subscriptions DROP CONSTRAINT IF EXISTS chk_billing_cycle;
ALTER TABLE subscriptions ADD CONSTRAINT chk_billing_cycle
    CHECK (billing_cycle IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'YEARLY'));
