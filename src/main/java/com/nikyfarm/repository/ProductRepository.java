package com.nikyfarm.repository;

import com.nikyfarm.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByCategoryId(Long categoryId);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("""
            select p
            from Product p
            where (:categoryId is null or p.category.id = :categoryId)
              and (
                    :search is null
                    or :search = ''
                    or lower(p.name) like lower(concat('%', :search, '%'))
                    or lower(p.slug) like lower(concat('%', :search, '%'))
                    or lower(coalesce(p.shortDescription, '')) like lower(concat('%', :search, '%'))
              )
            """)
    Page<Product> searchProducts(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            Pageable pageable);
}
