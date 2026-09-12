package com.nikyfarm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Deliberately has no {@code role}/{@code isActive} fields -- a user updating "themself" must never
 * be able to bind those, so the escalation-proofing is structural, not just a service-layer check
 * that could be forgotten later.
 */
@Data
public class UserSelfUpdateRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;
}
