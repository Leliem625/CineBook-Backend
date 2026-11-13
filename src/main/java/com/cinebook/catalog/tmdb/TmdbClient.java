package com.cinebook.catalog.tmdb;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.cinebook.catalog.tmdb.dto.TmdbDtos.MovieDetail;
import com.cinebook.catalog.tmdb.dto.TmdbDtos.MovieSummary;
import com.cinebook.catalog.tmdb.dto.TmdbDtos.Page;

/**
 * Client gọi TMDB API v3.
 * TODO: thêm Resilience4j (retry, timeout, rate limit) và cache Redis cho dữ liệu hiển thị.
 */
@Component
public class TmdbClient {

    private static final int MAX_PAGES = 10;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    private final RestClient rest;
    private final TmdbProperties props;

    public TmdbClient(RestClient.Builder builder, TmdbProperties props) {
        this.props = props;
        // Timeout bắt buộc: TMDB treo thì lỗi nhanh để service chuyển sang dữ liệu dự phòng
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        this.rest = builder
                .requestFactory(factory)
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.token())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    public boolean isConfigured() {
        return props.token() != null && !props.token().isBlank();
    }

    /** Phim đang chiếu tại VN (tất cả các trang). */
    public List<MovieSummary> nowPlaying() {
        return movieList("now_playing");
    }

    /** Phim sắp chiếu tại VN (tất cả các trang). */
    public List<MovieSummary> upcoming() {
        return movieList("upcoming");
    }

    /** Chi tiết phim kèm trailer, diễn viên, ngày chiếu/nhãn tuổi theo quốc gia (1 request). */
    public MovieDetail movieDetail(long tmdbId) {
        return rest.get()
                .uri(uri -> uri.path("/movie/{id}")
                        .queryParam("language", props.language())
                        .queryParam("append_to_response", "videos,credits,release_dates")
                        .queryParam("include_video_language", "vi,en")
                        .build(tmdbId))
                .retrieve()
                .body(MovieDetail.class);
    }

    private List<MovieSummary> movieList(String kind) {
        List<MovieSummary> all = new ArrayList<>();
        int totalPages = 1;
        for (int page = 1; page <= totalPages && page <= MAX_PAGES; page++) {
            int p = page;
            Page<MovieSummary> body = rest.get()
                    .uri(uri -> uri.path("/movie/" + kind)
                            .queryParam("region", props.region())
                            .queryParam("language", props.language())
                            .queryParam("page", p)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Page<MovieSummary>>() {
                    });
            if (body == null || body.results() == null) {
                break;
            }
            all.addAll(body.results());
            totalPages = body.totalPages();
        }
        return all;
    }
}
