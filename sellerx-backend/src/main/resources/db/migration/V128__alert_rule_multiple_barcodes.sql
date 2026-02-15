-- Allow multiple product barcodes (comma-separated) in alert rules
ALTER TABLE alert_rules ALTER COLUMN product_barcode TYPE TEXT;
