package com.cinebook.catalog.service;

import com.cinebook.catalog.entity.Movie;
import com.cinebook.catalog.tmdb.dto.TmdbDtos.MovieDetail;
import java.util.Optional;

/**
 * Lưu "bản gọn" của phim TMDB vào bảng movie (upsert theo tmdb_id).
 * <p>
 * Chỉ phim CÓ suất chiếu mới cần nằm trong DB: showtime cần khóa ngoại tới phim,
 * cần runtime để xếp lịch (BR-50) và cần tên phim ổn định cho vé/email.
 * Thông tin hiển thị chi tiết (mô tả, diễn viên, trailer) có thể gọi TMDB trực tiếp + cache.
 */
public interface MovieImportService {

    /**
     * Thêm mới hoặc cập nhật phim theo tmdb_id từ dữ liệu chi tiết của TMDB.
     * Phim admin đã sửa tay (manuallyEdited) thì giữ nguyên.
     */
    Movie upsert(MovieDetail detail);

    /** Tìm phim đã lưu theo id TMDB. */
    Optional<Movie> findByTmdbId(long tmdbId);
}
