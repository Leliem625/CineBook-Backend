-- =====================================================================
-- V6: Dữ liệu rạp giờ đến từ 2 nguồn (OpenStreetMap + Overture Maps).
-- Đổi osm_id thành source_id tổng quát: "osm:node/123", "overture:<uuid>".
-- =====================================================================
ALTER TABLE cinema RENAME COLUMN osm_id TO source_id;
ALTER TABLE cinema ALTER COLUMN source_id TYPE VARCHAR(100);
ALTER INDEX IF EXISTS cinema_osm_id_key RENAME TO cinema_source_id_key;
