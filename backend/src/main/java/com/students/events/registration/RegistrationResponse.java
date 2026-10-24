package com.students.events.registration;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long eventId,
        RegistrationStatus status,
        LocalDateTime registeredAt
) {
    public static RegistrationResponse from(EventRegistration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getStatus(),
                registration.getRegisteredAt()
        );
    }
}
