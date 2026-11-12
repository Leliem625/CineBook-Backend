-- Extension cần cho các quy tắc nghiệp vụ:
--   btree_gist : exclusion constraint chống chồng giờ suất chiếu (BR-50)
--   unaccent, pg_trgm : tìm kiếm không dấu, gần đúng
--   vector     : RAG cho trợ lý AI (pgvector)
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS vector;

-- Bảng nghiệp vụ được thêm theo từng giai đoạn: V2__catalog.sql, V3__cinema.sql, V4__showtime.sql, ...
