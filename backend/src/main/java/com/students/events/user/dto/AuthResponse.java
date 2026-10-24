package com.students.events.user.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String role
) {
}