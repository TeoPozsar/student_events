package com.students.events.event;

import com.students.events.event.dto.CreateEventRequest;
import com.students.events.user.User;
import com.students.events.user.UserRole;
import com.students.events.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceAuthorizationTest {

    EventRepository events = mock(EventRepository.class);
    EventPosterRepository posters = mock(EventPosterRepository.class);
    UserRepository users = mock(UserRepository.class);
    EventService service = new EventService(events, posters, users);
    Event event;

    private User user(long id, String email, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(u));
        return u;
    }

    private CreateEventRequest request(int capacity) {
        return new CreateEventRequest("T", "D", "L", "C", LocalDateTime.now().plusDays(1), capacity);
    }

    @BeforeEach
    void setUp() {
        User owner = user(1, "owner@x.ro", UserRole.ORGANIZER);
        event = new Event();
        event.setId(10L);
        event.setOrganizer(owner);
        event.setCapacity(10);
        event.setAttendeeCount(4);
        when(events.findById(10L)).thenReturn(Optional.of(event));
        when(events.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void assertStatus(HttpStatus expected, Runnable r) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, r::run);
        assertEquals(expected.value(), ex.getStatusCode().value());
    }

    @Test
    void otherOrganizerCannotUpdateOrDelete() {
        user(2, "other@x.ro", UserRole.ORGANIZER);
        assertStatus(HttpStatus.FORBIDDEN,
                () -> service.updateEvent(10L, request(10), null, true, "other@x.ro"));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.deleteEvent(10L, "other@x.ro"));
        verify(events, never()).delete(any());
        verify(posters, never()).deleteById(any());
    }

    @Test
    void plainUserCannotUpdateOrDelete() {
        user(3, "u@x.ro", UserRole.USER);
        assertStatus(HttpStatus.FORBIDDEN, () -> service.deleteEvent(10L, "u@x.ro"));
    }

    @Test
    void ownerAndAdminCanManage() {
        user(9, "admin@x.ro", UserRole.ADMIN);
        service.updateEvent(10L, request(10), null, false, "owner@x.ro");
        service.updateEvent(10L, request(10), null, false, "admin@x.ro");
        service.deleteEvent(10L, "owner@x.ro");
        verify(events).delete(event);
    }

    @Test
    void capacityCannotDropBelowAttendees() {
        assertStatus(HttpStatus.CONFLICT,
                () -> service.updateEvent(10L, request(3), null, false, "owner@x.ro"));
    }
}
