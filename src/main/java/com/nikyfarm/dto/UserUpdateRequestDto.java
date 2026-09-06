package com.nikyfarm.dto;

import com.nikyfarm.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin-only: unlike {@link UserSelfUpdateRequestDto}, this can change role and active status. */
@Data
public class UserUpdateRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @NotNull(message = "Role is required")
    private User.Role role;

    @NotNull(message = "Active status is required")
    private Boolean isActive;
}
