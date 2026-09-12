package com.nikyfarm.mapper;

import com.nikyfarm.dto.OrderItemResponseDto;
import com.nikyfarm.dto.OrderResponseDto;
import com.nikyfarm.entity.Order;
import com.nikyfarm.entity.OrderItems;
import java.util.Collections;
import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderResponseDto toResponseDto(Order order) {
        if (order == null) return null;

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .customerName(order.getUser() != null ? order.getUser().getName() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(
                        order.getShippingAddress() != null
                                ? AddressMapper.toResponseDto(order.getShippingAddress())
                                : null)
                .billingAddress(
                        order.getBillingAddress() != null
                                ? AddressMapper.toResponseDto(order.getBillingAddress())
                                : null)
                .items(
                        order.getItems() != null
                                ? order.getItems().stream()
                                        .map(OrderMapper::toItemResponseDto)
                                        .collect(Collectors.toList())
                                : Collections.emptyList())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public static OrderItemResponseDto toItemResponseDto(OrderItems item) {
        if (item == null) return null;

        return OrderItemResponseDto.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productVariantId(
                        item.getProductVariant() != null
                                ? item.getProductVariant().getId()
                                : null)
                .productNameSnapshot(item.getProductNameSnapshot())
                .variantLabelSnapshot(item.getVariantLabelSnapshot())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
