package com.yarden.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may contain only letters, digits, dot, underscore and hyphen")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        String password
) {}
