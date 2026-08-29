package com.ryan.socialplatform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record UserProfileUpdateRequest(
        @NotBlank(message = "First name must not be blank")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name must not be blank")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
        String avatarUrl,

        @Size(max = 2048, message = "Banner URL must not exceed 2048 characters")
        String bannerUrl,

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,

        @Size(max = 50, message = "Pronouns must not exceed 50 characters")
        String pronouns,

        @Size(max = 100, message = "Location must not exceed 100 characters")
        String location,

        @Size(max = 500, message = "Website URL must not exceed 500 characters")
        @URL(message = "Website URL must be a valid URL")
        String websiteUrl,

        @Past(message = "Birthday must be in the past")
        LocalDate birthday,

        @Size(max = 100, message = "Pronunciation must not exceed 100 characters")
        String pronunciation
) {
}
