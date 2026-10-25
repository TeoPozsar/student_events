package com.students.events.user.dto;

import com.students.events.user.User;

public record ProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        String university,
        String location,
        boolean hasProfilePicture
) {

    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getUniversity(),
                user.getLocation(),
                user.getProfilePicture() != null && user.getProfilePicture().length > 0
        );
    }
}
