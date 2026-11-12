package com.cinebook.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Phim (đồng bộ từ TMDB - FR-105). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Integer tmdbId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String titleVi;

    private String originalTitle;

    @Column(columnDefinition = "text")
    private String overview;

    private Integer runtimeMin;

    private LocalDate releaseDateVn;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private AgeRating ageRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieStatus status = MovieStatus.COMING_SOON;

    @Column(length = 100)
    private String country;

    @Column(length = 10)
    private String originalLanguage;

    @Column(length = 500)
    private String posterUrl;

    @Column(length = 500)
    private String backdropUrl;

    @Column(length = 50)
    private String trailerKey;

    @Column(columnDefinition = "text")
    private String directors;

    @Column(columnDefinition = "text")
    private String casts;

    @Column(precision = 3, scale = 1)
    private BigDecimal tmdbScore;

    @Column(precision = 12, scale = 3)
    private BigDecimal tmdbPopularity;

    /** true: admin đã sửa tay, job đồng bộ TMDB không ghi đè. */
    @Column(nullable = false)
    private boolean manuallyEdited;

    private Instant syncedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(name = "movie_genre",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();
}
