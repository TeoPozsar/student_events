
package com.students.events.event;

import com.students.events.event.dto.CreateEventRequest;
import com.students.events.user.User;
import com.students.events.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository
    ) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));
    }

    @Transactional
    public Event createEvent(
            CreateEventRequest request,
            String organizerEmail
    ) {
        User organizer = userRepository
                .findByEmailIgnoreCase(organizerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organizer not found"
                ));

        Event event = new Event();
        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setLocation(request.location().trim());
        event.setCategory(request.category().trim());
        event.setStartDateTime(request.startDateTime());
        event.setCapacity(request.capacity());
        event.setAttendeeCount(0);
        event.setOrganizer(organizer);

        return eventRepository.save(event);
    }
}
