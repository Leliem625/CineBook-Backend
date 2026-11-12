-- =====================================================================
-- V5: Lưu nguồn dữ liệu rạp từ OpenStreetMap (© OpenStreetMap contributors, ODbL)
-- Dùng để đồng bộ lại snapshot mà không tạo rạp trùng.
-- =====================================================================
ALTER TABLE cinema ADD COLUMN osm_id VARCHAR(30) UNIQUE;   -- ví dụ "node/123456", "way/789"
