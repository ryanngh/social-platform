package com.ryan.socialplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Identifier must not be blank")
        String identifier,   // email hoặc phoneNumber

        @NotBlank(message = "Password must not be blank")
        String password
) {
}
