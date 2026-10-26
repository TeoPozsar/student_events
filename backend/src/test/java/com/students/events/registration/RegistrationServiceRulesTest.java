package com.students.events.registration;

import com.students.events.event.Event;
import com.students.events.event.EventRepository;
import com.students.events.user.User;
import com.students.events.user.UserRole;
import com.students.events.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class RegistrationServiceRulesTest {

    EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
    EventRepository events = mock(EventRepository.class);
    UserRepository users = mock(UserRepository.class);
    RegistrationService service = new RegistrationService(registrations, events, users);
    Event event;

    private User user(long id, String email, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(u));
        return u;
    }

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(5L);
        event.setCapacity(1);
        event.setAttendeeCount(0);
        when(events.findByIdForUpdate(5L)).thenReturn(Optional.of(event));
        when(registrations.findByUserIdAndEventId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(registrations.save(any(EventRegistration.class))).thenAnswer(i -> i.getArgument(0));
    }

    private int statusOf(String email) {
        return assertThrows(ResponseStatusException.class, () -> service.register(5L, email))
                .getStatusCode().value();
    }

    @Test
    void onlyUserRoleCanRegister() {
        user(1, "org@x.ro", UserRole.ORGANIZER);
        user(2, "adm@x.ro", UserRole.ADMIN);
        assertEquals(HttpStatus.FORBIDDEN.value(), statusOf("org@x.ro"));
        assertEquals(HttpStatus.FORBIDDEN.value(), statusOf("adm@x.ro"));
        verify(registrations, never()).save(any());
    }

    @Test
    void userCanRegister() {
        user(8, "ok@x.ro", UserRole.USER);
        assertEquals(RegistrationStatus.REGISTERED, service.register(5L, "ok@x.ro").getStatus());
        assertEquals(1, event.getAttendeeCount());
    }

    @Test
    void adminCannotCancelRegistration() {
        user(9, "adm2@x.ro", UserRole.ADMIN);
        assertEquals(HttpStatus.FORBIDDEN.value(),
                assertThrows(ResponseStatusException.class, () -> service.cancel(5L, "adm2@x.ro"))
                        .getStatusCode().value());
        verify(registrations, never()).save(any());
    }

    @Test
    void duplicateRegistrationIsRejected() {
        User u = user(3, "u@x.ro", UserRole.USER);
        EventRegistration existing = new EventRegistration();
        existing.setUser(u);
        existing.setEvent(event);
        existing.setStatus(RegistrationStatus.REGISTERED);
        when(registrations.findByUserIdAndEventId(3L, 5L)).thenReturn(Optional.of(existing));
        assertEquals(HttpStatus.CONFLICT.value(), statusOf("u@x.ro"));
        assertEquals(0, event.getAttendeeCount());
    }

    @Test
    void fullEventIsRejected() {
        user(4, "a@x.ro", UserRole.USER);
        user(5, "b@x.ro", UserRole.USER);
        service.register(5L, "a@x.ro");
        assertEquals(1, event.getAttendeeCount());
        assertEquals(HttpStatus.CONFLICT.value(), statusOf("b@x.ro"));
        assertEquals(1, event.getAttendeeCount());
    }

    @Test
    void myRegistrationsAreQueriedByAuthenticatedUserId() {
        user(7, "me@x.ro", UserRole.USER);
        when(registrations.findByUserIdAndStatusNotOrderByRegisteredAtDesc(7L, RegistrationStatus.CANCELLED))
                .thenReturn(List.of());
        service.findActiveForUser("me@x.ro");
        verify(registrations).findByUserIdAndStatusNotOrderByRegisteredAtDesc(7L, RegistrationStatus.CANCELLED);
    }
}
