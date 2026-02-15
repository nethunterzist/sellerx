-- Trendyol iade durumunda komisyonu geri veriyor
-- totalLoss'tan commissionLoss cikarilmali
UPDATE return_records
SET total_loss = total_loss - COALESCE(commission_loss, 0),
    updated_at = NOW()
WHERE commission_loss > 0 AND total_loss > 0;
