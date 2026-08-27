package com.nikyfarm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products",
        indexes = {
                @Index(name = "idx_products_category ", columnList = "category_id"),
                @Index(name = "idx_products_active", columnList = "is_active")
        })
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "slug")
    private String slug;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "description")
    private String description;

    @Column(name = "base_price")
    private Double basePrice;

    @Column(name = "discounted_price")
    private Double discountedPrice;

    @Column(name = "avg_rating")
    private Float avgRating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", orphanRemoval = true)
    private List<ProductImage> productImages = new ArrayList<>();

    @OneToMany(mappedBy = "product", orphanRemoval = true)
    private Set<ProductVariant> productVariants = new LinkedHashSet<>();

}
