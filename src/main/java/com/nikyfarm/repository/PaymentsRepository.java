package com.nikyfarm.repository;

import com.nikyfarm.entity.Payments;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentsRepository extends JpaRepository<Payments, Long> {

    Optional<Payments> findByProviderOrderId(String providerOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                  select p
                  from Payments p
                  join fetch p.order o
                  join fetch o.user
                  where p.providerOrderId = :providerOrderId
                  """)
    Optional<Payments> findByProviderOrderIdForUpdate(@Param("providerOrderId") String providerOrderId);

    Optional<Payments> findByProviderPaymentId(String providerPaymentId);

    List<Payments> findByOrderId(Long orderId);

    List<Payments> findByOrderIdAndStatusIn(Long orderId, List<Payments.Status> statuses);

    Optional<Payments> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                  select p
                  from Payments p
                  join fetch p.order o
                  join fetch o.user
                  where p.id = :paymentId
                  """)
    Optional<Payments> findByIdForPaymentUpdate(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                  select p
                  from Payments p
                  join fetch p.order o
                  join fetch o.user
                  where o.id = :orderId
                  and p.status = :status
                  order by p.createdAt desc
                  """)
    List<Payments> findByOrderIdAndStatusForUpdate(
            @Param("orderId") Long orderId, @Param("status") Payments.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
                  select p
                  from Payments p
                  join fetch p.order o
                  join fetch o.user
                  where o.id = :orderId
                  and p.providerPaymentId is not null
                  order by p.createdAt desc
                  """)
    List<Payments> findProviderPaymentsByOrderIdForUpdate(@Param("orderId") Long orderId);
}
