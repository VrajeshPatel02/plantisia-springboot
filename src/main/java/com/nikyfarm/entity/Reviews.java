package com.nikyfarm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "reviews",
        indexes = {@Index(name = "idx_reviews_product", columnList = "product_id")})
public class Reviews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "title")
    private String title;

    @Column(name = "comment")
    private String comment;

    @Column(name = "is_verified_purchase")
    private Boolean isVerifiedPurchase;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Legacy single-image relationship.
     * <p>
     * Kept temporarily for backward compatibility while existing
     * review_images data is migrated to review_media.
     */
    @ManyToOne
    @JoinColumn(name = "review_image_id")
    private ReviewImage reviewImage;

    /**
     * New review media collection.
     * <p>
     * A review can contain multiple images and/or videos.
     */
    @OneToMany(
            mappedBy = "review",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ReviewMedia> media = new ArrayList<>();
}