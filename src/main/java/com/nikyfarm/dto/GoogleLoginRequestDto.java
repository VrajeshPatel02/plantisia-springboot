package com.nikyfarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequestDto {

    @NotBlank(message = "Google ID token is required")
    private String idToken;
}