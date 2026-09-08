package com.nikyfarm.mapper;

import com.nikyfarm.dto.CartItemResponseDto;
import com.nikyfarm.dto.CartResponseDto;
import com.nikyfarm.entity.Cart;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class CartMapper {

    private CartMapper() {
    }

    public static CartResponseDto toResponseDto(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponseDto> itemDtos = cart.getItems().stream()
                .map(CartItemMapper::toResponseDto)
                .sorted(Comparator.comparing(CartItemResponseDto::getAddedAt))
                .collect(Collectors.toList());

        int totalItemCount = itemDtos.stream()
                .mapToInt(CartItemResponseDto::getQuantity)
                .sum();

        BigDecimal totalPrice = itemDtos.stream()
                .map(CartItemResponseDto::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponseDto.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(itemDtos)
                .totalItemCount(totalItemCount)
                .totalPrice(totalPrice)
                .build();
    }
}
