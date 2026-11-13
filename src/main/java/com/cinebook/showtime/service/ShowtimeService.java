package com.cinebook.showtime.service;

import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.showtime.dto.MovieShowtimesResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Tra cứu lịch chiếu (cache Redis).
 */
public interface ShowtimeService {

    /**
     * Các phim có suất chiếu trong 1 ngày (theo giờ Việt Nam), mỗi phim kèm danh sách suất sắp theo giờ.
     * Phim xếp theo giờ chiếu sớm nhất. Không gồm suất đã huỷ. Không có suất nào thì trả danh sách rỗng.
     *
     * @param provinceSlug lọc theo tỉnh; slug không tồn tại -> 404
     * @param cinemaId     lọc theo cụm rạp; rạp không tồn tại -> 404
     * @param date         ngày cần xem (bắt buộc)
     * @param movieId      lọc theo phim (null = mọi phim)
     * @param format       lọc theo định dạng (null = mọi định dạng)
     *                     Phải có ít nhất provinceSlug hoặc cinemaId, nếu không -> 400.
     */
    List<MovieShowtimesResponse> getShowtimes(String provinceSlug, Long cinemaId, LocalDate date,
                                                 Long movieId, ScreenFormat format);
}
