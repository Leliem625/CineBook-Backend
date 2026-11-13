package com.cinebook.catalog.service;

import com.cinebook.catalog.dto.MovieResponse;
import com.cinebook.catalog.entity.MovieStatus;

import java.util.Collection;
import java.util.List;

/**
 * Nghiệp vụ phim.
 */
public interface MovieService {

    /**
     * Danh sách phim theo trạng thái, phim hot (popularity TMDB cao) xếp trước (FR-101).
     *
     * @param status trạng thái cần lấy; null = đang chiếu + sắp chiếu
     */
    List<MovieResponse> getMovies(MovieStatus status);

    /** Phim theo danh sách id nội bộ (đọc DB, không gọi TMDB); id không tồn tại thì bỏ qua. */
    List<MovieResponse> getMoviesByIds(Collection<Long> ids);

    // TODO getBySlug (FR-103), lọc theo thể loại, phân trang, cập nhật trạng thái chiếu (BR-53)
}
