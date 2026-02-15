-- Normalize city names in trendyol_orders table
-- This migration fixes duplicate cities caused by:
-- 1. Trailing/leading whitespace differences
-- 2. Case differences (KAYSERI vs Kayseri)
-- 3. Turkish character variations (Istanbul vs İstanbul)

-- Step 1: Trim whitespace from all city names
UPDATE trendyol_orders
SET shipment_city = TRIM(shipment_city)
WHERE shipment_city IS NOT NULL
  AND shipment_city != TRIM(shipment_city);

-- Step 2: Convert to uppercase (for consistency with Java normalization)
-- Note: PostgreSQL UPPER() handles Turkish characters correctly with UTF-8 encoding
UPDATE trendyol_orders
SET shipment_city = UPPER(shipment_city)
WHERE shipment_city IS NOT NULL
  AND shipment_city != UPPER(shipment_city);

-- Step 3: Fix common Turkish character issues (Istanbul → İSTANBUL)
-- These explicit fixes ensure proper Turkish character handling
UPDATE trendyol_orders
SET shipment_city = 'İSTANBUL'
WHERE shipment_city IN ('ISTANBUL', 'Istanbul', 'istanbul');

UPDATE trendyol_orders
SET shipment_city = 'İZMİR'
WHERE shipment_city IN ('IZMIR', 'Izmir', 'izmir');

UPDATE trendyol_orders
SET shipment_city = 'ŞANLIURFA'
WHERE shipment_city IN ('SANLIURFA', 'Sanliurfa', 'sanliurfa', 'ŞANLIURFA');

UPDATE trendyol_orders
SET shipment_city = 'ŞIRNAK'
WHERE shipment_city IN ('SIRNAK', 'Sirnak', 'sirnak');

UPDATE trendyol_orders
SET shipment_city = 'MUĞLA'
WHERE shipment_city IN ('MUGLA', 'Mugla', 'mugla');

UPDATE trendyol_orders
SET shipment_city = 'AĞRI'
WHERE shipment_city IN ('AGRI', 'Agri', 'agri');

UPDATE trendyol_orders
SET shipment_city = 'ÇANAKKALE'
WHERE shipment_city IN ('CANAKKALE', 'Canakkale', 'canakkale');

UPDATE trendyol_orders
SET shipment_city = 'ÇANKIRI'
WHERE shipment_city IN ('CANKIRI', 'Cankiri', 'cankiri');

UPDATE trendyol_orders
SET shipment_city = 'ÇORUM'
WHERE shipment_city IN ('CORUM', 'Corum', 'corum');

UPDATE trendyol_orders
SET shipment_city = 'DÜZCE'
WHERE shipment_city IN ('DUZCE', 'Duzce', 'duzce');

UPDATE trendyol_orders
SET shipment_city = 'GÜMÜŞHANE'
WHERE shipment_city IN ('GUMUSHANE', 'Gumushane', 'gumushane');

UPDATE trendyol_orders
SET shipment_city = 'IĞDIR'
WHERE shipment_city IN ('IGDIR', 'Igdir', 'igdir');

UPDATE trendyol_orders
SET shipment_city = 'KAHRAMANMARAŞ'
WHERE shipment_city IN ('KAHRAMANMARAS', 'Kahramanmaras', 'kahramanmaras');

UPDATE trendyol_orders
SET shipment_city = 'KÜTAHYA'
WHERE shipment_city IN ('KUTAHYA', 'Kutahya', 'kutahya');

UPDATE trendyol_orders
SET shipment_city = 'NEVŞEHİR'
WHERE shipment_city IN ('NEVSEHIR', 'Nevsehir', 'nevsehir');

UPDATE trendyol_orders
SET shipment_city = 'NİĞDE'
WHERE shipment_city IN ('NIGDE', 'Nigde', 'nigde');

UPDATE trendyol_orders
SET shipment_city = 'UŞAK'
WHERE shipment_city IN ('USAK', 'Usak', 'usak');

-- Add index for faster city-based queries if not exists
CREATE INDEX IF NOT EXISTS idx_trendyol_orders_shipment_city_normalized
ON trendyol_orders (shipment_city)
WHERE shipment_city IS NOT NULL;

-- Log migration completion
DO $$
DECLARE
    city_count INTEGER;
BEGIN
    SELECT COUNT(DISTINCT shipment_city) INTO city_count
    FROM trendyol_orders
    WHERE shipment_city IS NOT NULL;

    RAISE NOTICE 'City normalization complete. Unique cities: %', city_count;
END $$;
