package com.nikyfarm.repository;

import com.nikyfarm.entity.Order;
import com.nikyfarm.entity.OrderItems;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

    List<OrderItems> findByOrderId(Long orderId);

    @Query("""
            SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END
            FROM OrderItems oi
            WHERE oi.order.user.id = :userId
              AND oi.product.id = :productId
              AND oi.order.paymentStatus = :paymentStatus
            """)
    boolean existsPaidPurchase(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("paymentStatus") Order.PaymentStatus paymentStatus);
}
