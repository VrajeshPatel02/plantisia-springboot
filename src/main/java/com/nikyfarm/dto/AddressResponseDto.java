package com.nikyfarm.dto;

import java.time.LocalDateTime;

public record AddressResponseDto(
        Long id,
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        Boolean isDefault,
        LocalDateTime createdAt,
        Long userId
) {
}
