package com.nikyfarm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_images",
        indexes = {
                @Index(name = "idx_product_images_product", columnList = "product_id")
        })
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_primary")
    private Boolean isPrimary;

}
