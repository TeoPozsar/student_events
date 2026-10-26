package com.students.events.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    // Registrations of any status reference the event through a foreign key.
    @Query("select count(r) > 0 from EventRegistration r where r.event.id = :id")
    boolean hasRegistrations(@Param("id") Long id);

    // Discovery: everything that has not ended yet (upcoming and in progress).
    @EntityGraph(attributePaths = "organizer")
    List<Event> findByEndDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime now);

    @EntityGraph(attributePaths = "organizer")
    List<Event> findByOrganizerIdOrderByStartDateTimeDesc(Long organizerId);

    @EntityGraph(attributePaths = "organizer")
    List<Event> findAllByOrderByStartDateTimeDesc();
}