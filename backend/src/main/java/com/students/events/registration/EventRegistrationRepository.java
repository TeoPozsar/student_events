
package com.students.events.registration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRegistrationRepository
        extends JpaRepository<EventRegistration, Long> {

    Optional<EventRegistration> findByUserIdAndEventId(
            Long userId,
            Long eventId
    );

    boolean existsByUserIdAndEventIdAndStatus(
            Long userId,
            Long eventId,
            RegistrationStatus status
    );
}
