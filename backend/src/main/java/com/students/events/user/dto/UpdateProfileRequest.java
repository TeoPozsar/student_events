package com.students.events.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Deliberately has no role or email field: those cannot be changed through the profile.
public record UpdateProfileRequest(

        @NotBlank
        @Size(max = 50)
        String firstName,

        @NotBlank
        @Size(max = 50)
        String lastName,

        @Size(max = 200)
        String university,

        @Size(max = 100)
        String location
) {
}
