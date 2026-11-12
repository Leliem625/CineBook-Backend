package com.cinebook.cinema.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cụm rạp.
 * provinceId chỉ lưu id (không map quan hệ JPA) vì Province thuộc module catalog:
 * module khác nhau tham chiếu nhau bằng id để dễ tách service sau này.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "cinema")
public class Cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chain_id")
    private CinemaChain chain;

    @Column(nullable = false)
    private Long provinceId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(length = 20)
    private String phone;

    /** Id bản ghi OpenStreetMap, ví dụ "node/123456" (null nếu admin tạo tay). */
    @Column(unique = true, length = 30)
    private String osmId;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
