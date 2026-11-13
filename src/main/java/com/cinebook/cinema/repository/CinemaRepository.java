package com.cinebook.cinema.repository;

import com.cinebook.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    /** Số cụm rạp đang hoạt động theo tỉnh (1 câu GROUP BY, không tải entity). */
    interface ProvinceCount {
        Long getProvinceId();

        long getTotal();
    }

    @Query("SELECT c.provinceId AS provinceId, COUNT(c) AS total FROM Cinema c WHERE c.active = true GROUP BY c.provinceId")
    List<ProvinceCount> countActiveByProvince();

    /** Rạp đang hoạt động, nạp kèm hệ thống rạp trong cùng câu SQL (tránh N+1). */
    @EntityGraph(attributePaths = "chain")
    List<Cinema> findByActiveTrue();

    @EntityGraph(attributePaths = "chain")
    List<Cinema> findByActiveTrueAndProvinceId(Long provinceId);
}
