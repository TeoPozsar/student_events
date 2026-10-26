package com.students.events.event;

import com.students.events.event.dto.CreateEventRequest;
import com.students.events.user.User;
import com.students.events.user.UserRole;
import com.students.events.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventListingTest {

    static final LocalDateTime START = LocalDateTime.of(2030, 1, 1, 10, 0);
    static final LocalDateTime END = LocalDateTime.of(2030, 1, 1, 12, 0);

    EventRepository events = mock(EventRepository.class);
    UserRepository users = mock(UserRepository.class);
    EventService service = new EventService(events, mock(EventPosterRepository.class), users);

    private User user(long id, String email, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void statusBoundaries() {
        assertEquals(EventStatus.UPCOMING, EventStatus.of(START, END, START.minusNanos(1)));
        assertEquals(EventStatus.IN_PROGRESS, EventStatus.of(START, END, START));
        assertEquals(EventStatus.IN_PROGRESS, EventStatus.of(START, END, END.minusNanos(1)));
        assertEquals(EventStatus.FINISHED, EventStatus.of(START, END, END));
        assertEquals(EventStatus.FINISHED, EventStatus.of(START, END, END.plusDays(1)));
    }

    @Test
    void eventExposesDerivedStatus() {
        Event past = new Event();
        past.setStartDateTime(LocalDateTime.now().minusHours(3));
        past.setEndDateTime(LocalDateTime.now().minusHours(1));
        Event running = new Event();
        running.setStartDateTime(LocalDateTime.now().minusHours(1));
        running.setEndDateTime(LocalDateTime.now().plusHours(1));
        Event future = new Event();
        future.setStartDateTime(LocalDateTime.now().plusHours(1));
        future.setEndDateTime(LocalDateTime.now().plusHours(2));

        assertEquals(EventStatus.FINISHED, past.getStatus());
        assertEquals(EventStatus.IN_PROGRESS, running.getStatus());
        assertEquals(EventStatus.UPCOMING, future.getStatus());
    }

    @Test
    void discoveryQueriesOnlyEventsEndingAfterNow() {
        Event running = new Event();
        when(events.findByEndDateTimeAfterOrderByStartDateTimeAsc(any())).thenReturn(List.of(running));

        LocalDateTime before = LocalDateTime.now();
        assertEquals(List.of(running), service.getDiscoverableEvents());

        var captor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(events).findByEndDateTimeAfterOrderByStartDateTimeAsc(captor.capture());
        assertFalse(captor.getValue().isBefore(before));
        verify(events, never()).findAll();
    }

    @Test
    void organizerListUsesAuthenticatedOrganizerIdOnly() {
        user(1, "a@x.ro", UserRole.ORGANIZER);
        Event own = new Event();
        when(events.findByOrganizerIdOrderByStartDateTimeDesc(1L)).thenReturn(List.of(own));

        assertEquals(List.of(own), service.getOrganizerEvents("a@x.ro"));
        verify(events, never()).findByOrganizerIdOrderByStartDateTimeDesc(2L);
    }

    @Test
    void nonOrganizerCannotUseMyEvents() {
        user(2, "u@x.ro", UserRole.USER);
        user(3, "adm@x.ro", UserRole.ADMIN);

        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.getOrganizerEvents("u@x.ro")).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.getOrganizerEvents("adm@x.ro")).getStatusCode().value());
    }

    @Test
    void adminSeesAllEventsAndOthersAreRejected() {
        user(3, "adm@x.ro", UserRole.ADMIN);
        user(1, "org@x.ro", UserRole.ORGANIZER);
        Event e = new Event();
        when(events.findAllByOrderByStartDateTimeDesc()).thenReturn(List.of(e, new Event()));

        assertEquals(2, service.getAllEventsForAdmin("adm@x.ro").size());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.getAllEventsForAdmin("org@x.ro")).getStatusCode().value());
    }

    @Test
    void createRejectsPastStart() {
        user(1, "org@x.ro", UserRole.ORGANIZER);
        var request = new CreateEventRequest("T", "D", "L", "Technology",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1), 5);

        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.createEvent(request, "org@x.ro", null)).getStatusCode().value());
    }

    @Test
    void endMustBeAfterStart() {
        var bad = new CreateEventRequest("T", "D", "L", "Technology", START, START, 5);
        var good = new CreateEventRequest("T", "D", "L", "Technology", START, END, 5);

        assertFalse(bad.isEndDateTimeAfterStart());
        assertTrue(good.isEndDateTimeAfterStart());
    }
}
