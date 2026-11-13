package com.cinebook.catalog.repository;

import com.cinebook.catalog.entity.Movie;
import com.cinebook.catalog.entity.MovieStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByTmdbId(Integer tmdbId);

    boolean existsBySlug(String slug);

    /**
     * Phim theo trạng thái. @EntityGraph nạp luôn genres trong cùng 1 câu SQL (JOIN),
     * tránh lỗi N+1 (mỗi phim lại chạy thêm 1 query lấy thể loại).
     */
    @EntityGraph(attributePaths = "genres")
    List<Movie> findByStatusInOrderByTmdbPopularityDesc(Collection<MovieStatus> statuses);

    /** Phim đã lưu ứng với danh sách id TMDB (dùng để ghép dữ liệu nội bộ vào kết quả TMDB). */
    @EntityGraph(attributePaths = "genres")
    List<Movie> findByTmdbIdIn(Collection<Integer> tmdbIds);
}
