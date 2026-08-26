package com.ryan.socialplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Phone number must not be blank")
        @Size(max = 15, message = "Phone number must not exceed 15 characters")
        String phoneNumber,

        @Email(message = "Email must be a valid email address")
        String email, // optional — có thể null

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "First name must not be blank")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name must not be blank")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName
) {
}