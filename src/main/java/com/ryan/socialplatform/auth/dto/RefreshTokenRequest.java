// auth/dto/RefreshTokenRequest.java
package com.ryan.socialplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token must not be blank")
        String refreshToken
) {
}