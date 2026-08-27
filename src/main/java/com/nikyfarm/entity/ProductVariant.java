package com.nikyfarm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_variants",
        indexes = {
                @Index(name = "idx_variants_product", columnList = "product_id")
        })
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variant_label")
    private String variantLabel;

    @Column(name = "size")
    private String size;

    @Column(name = "colour")
    private String colour;

    @Column(name = "price")
    private Double price;

    @Column(name = "discounted_price")
    private Double discountedPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "sku", length = 80)
    private String sku;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
