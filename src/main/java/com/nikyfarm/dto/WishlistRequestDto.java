package com.nikyfarm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WishlistRequestDto {

    @NotNull(message = "Product id is required")
    private Long productId;
}
