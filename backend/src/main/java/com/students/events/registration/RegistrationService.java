
package com.students.events.registration;

import com.students.events.event.Event;
import com.students.events.event.EventRepository;
import com.students.events.user.User;
import com.students.events.user.UserRole;
import com.students.events.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class RegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public RegistrationService(
            EventRegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository
    ) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public java.util.List<RegistrationResponse> findActiveForUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        return registrationRepository
                .findByUserIdAndStatusNotOrderByRegisteredAtDesc(
                        user.getId(), RegistrationStatus.CANCELLED)
                .stream()
                .map(RegistrationResponse::from)
                .toList();
    }

    @Transactional
    public EventRegistration register(Long eventId, String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != UserRole.USER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only users with role USER can register for events");
        }

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        EventRegistration existing = registrationRepository
                .findByUserIdAndEventId(user.getId(), eventId)
                .orElse(null);

        if (existing != null) {
            if (existing.getStatus() != RegistrationStatus.CANCELLED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Already registered for this event");
            }

            if (event.getAttendeeCount() >= event.getCapacity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Event is full");
            }

            existing.setStatus(RegistrationStatus.REGISTERED);
            existing.setRegisteredAt(LocalDateTime.now());
            event.setAttendeeCount(event.getAttendeeCount() + 1);

            eventRepository.save(event);
            return registrationRepository.save(existing);
        }

        if (event.getAttendeeCount() >= event.getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Event is full");
        }

        EventRegistration registration = new EventRegistration();
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(LocalDateTime.now());

        event.setAttendeeCount(event.getAttendeeCount() + 1);

        eventRepository.save(event);
        return registrationRepository.save(registration);
    }

    @Transactional
    public void cancel(Long eventId, String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        EventRegistration registration = registrationRepository
                .findByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Registration not found"));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Registration already cancelled");
        }

        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot cancel a checked-in registration");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        event.setAttendeeCount(Math.max(0, event.getAttendeeCount() - 1));

        registrationRepository.save(registration);
        eventRepository.save(event);
    }
}
