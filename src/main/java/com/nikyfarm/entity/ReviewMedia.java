package com.nikyfarm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_media", indexes = {
        @Index(name = "idx_review_media_review", columnList = "review_id"),
        @Index(name = "idx_review_media_type", columnList = "media_type"),
        @Index(name = "idx_review_media_sort", columnList = "review_id, sort_order")
})
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Reviews review;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private ReviewMediaType mediaType;

    @Column(name = "media_url", nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "public_id", nullable = false, length = 500)
    private String publicId;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
