package com.cinebook.cinema.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.EnumSet;
import java.util.Set;

/** Phòng chiếu. gridRows/gridCols là kích thước lưới sơ đồ (gồm cả lối đi). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer gridRows;

    @Column(nullable = false)
    private Integer gridCols;

    @Column(nullable = false)
    private boolean active = true;

    /** Định dạng phòng hỗ trợ (BR-51). */
    @ElementCollection
    @CollectionTable(name = "room_format", joinColumns = @JoinColumn(name = "room_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private Set<ScreenFormat> formats = EnumSet.noneOf(ScreenFormat.class);
}
