-- Remove stock tracking feature entirely
-- Stock tracking used HTML scraping of Trendyol product pages which:
-- 1. Risks IP bans from Trendyol (thousands of requests from single IP)
-- 2. Cannot scale beyond 500+ stores (1 req/sec = 83+ min per hourly cycle)
-- 3. Consumes ~5-10 GB bandwidth per hour
-- 4. Violates Trendyol Terms of Service

DROP TABLE IF EXISTS stock_alerts CASCADE;
DROP TABLE IF EXISTS stock_snapshots CASCADE;
DROP TABLE IF EXISTS stock_tracked_products CASCADE;
