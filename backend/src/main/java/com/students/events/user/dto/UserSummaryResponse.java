package com.students.events.user.dto;

import com.students.events.user.User;

/** Safe projection of a user for administration screens: no password or other sensitive data. */
public record UserSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role
) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
