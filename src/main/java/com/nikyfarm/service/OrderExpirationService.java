package com.nikyfarm.service;

import com.nikyfarm.entity.Order;
import com.nikyfarm.entity.Order.OrderStatus;
import com.nikyfarm.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationService {

    private static final int UNPAID_ORDER_EXPIRY_MINUTES = 30;

    private final OrderRepository orderRepository;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireUnpaidOrders() {

        LocalDateTime cutoff =
                LocalDateTime.now()
                        .minusMinutes(UNPAID_ORDER_EXPIRY_MINUTES);

        List<Order> expiredOrders =
                orderRepository.findExpiredUnpaidOrders(
                        OrderStatus.PENDING,
                        Order.PaymentStatus.UNPAID,
                        cutoff);

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (Order order : expiredOrders) {

            order.setStatus(OrderStatus.CANCELLED);

            log.info(
                    "Expired unpaid order: {}",
                    order.getOrderNumber());
        }

        orderRepository.saveAll(expiredOrders);

        log.info(
                "Expired {} unpaid order(s)",
                expiredOrders.size());
    }
}