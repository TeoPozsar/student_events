
package com.students.events.event;

import com.students.events.event.dto.CreateEventRequest;
import com.students.events.user.User;
import com.students.events.user.UserRole;
import com.students.events.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EventService {

    private static final long MAX_POSTER_BYTES = 5L * 1024 * 1024;

    private final EventRepository eventRepository;
    private final EventPosterRepository posterRepository;
    private final UserRepository userRepository;

    public EventService(
            EventRepository eventRepository,
            EventPosterRepository posterRepository,
            UserRepository userRepository
    ) {
        this.eventRepository = eventRepository;
        this.posterRepository = posterRepository;
        this.userRepository = userRepository;
    }

    // Discovery list: events that have not ended yet (UPCOMING and IN_PROGRESS).
    @Transactional(readOnly = true)
    public List<Event> getDiscoverableEvents() {
        return eventRepository.findByEndDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime.now());
    }

    // The organizer is resolved from the authenticated principal; all statuses are included.
    @Transactional(readOnly = true)
    public List<Event> getOrganizerEvents(String organizerEmail) {
        User organizer = userRepository.findByEmailIgnoreCase(organizerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
        if (organizer.getRole() != UserRole.ORGANIZER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only organizers have their own events");
        }
        return eventRepository.findByOrganizerIdOrderByStartDateTimeDesc(organizer.getId());
    }

    @Transactional(readOnly = true)
    public List<Event> getAllEventsForAdmin(String actorEmail) {
        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
        if (actor.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Administrator access required");
        }
        return eventRepository.findAllByOrderByStartDateTimeDesc();
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
            String organizerEmail,
            MultipartFile poster
    ) {
        PosterFile validPoster = validatePoster(poster);
        requireStartInFuture(request.startDateTime());

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
        event.setEndDateTime(request.endDateTime());
        event.setCapacity(request.capacity());
        event.setAttendeeCount(0);
        event.setOrganizer(organizer);

        Event saved = eventRepository.save(event);
        if (validPoster != null) {
            storePoster(saved, validPoster);
        }
        return saved;
    }

    @Transactional
    public Event updateEvent(
            Long id,
            CreateEventRequest request,
            MultipartFile poster,
            boolean removePoster,
            String actorEmail
    ) {
        PosterFile validPoster = validatePoster(poster);
        Event event = getEventById(id);
        requireCanManage(event, actorEmail);

        if (request.capacity() < event.getAttendeeCount()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Capacity cannot be lower than the current number of attendees"
            );
        }

        // Only a changed start must be in the future, so running or finished events stay editable.
        // The form sends minute precision, so compare at that precision.
        if (event.getStartDateTime() == null || !request.startDateTime().truncatedTo(ChronoUnit.MINUTES)
                .equals(event.getStartDateTime().truncatedTo(ChronoUnit.MINUTES))) {
            requireStartInFuture(request.startDateTime());
        }

        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setLocation(request.location().trim());
        event.setCategory(request.category().trim());
        event.setStartDateTime(request.startDateTime());
        event.setEndDateTime(request.endDateTime());
        event.setCapacity(request.capacity());

        if (validPoster != null) {
            storePoster(event, validPoster);
        } else if (removePoster) {
            posterRepository.deleteById(event.getId());
            event.setPosterUpdatedAt(null);
        }

        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long id, String actorEmail) {
        Event event = getEventById(id);
        requireCanManage(event, actorEmail);
        if (eventRepository.hasRegistrations(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This event has registrations and cannot be deleted"
            );
        }
        eventRepository.delete(event);
    }

    private void requireStartInFuture(LocalDateTime start) {
        if (!start.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Start time must be in the future");
        }
    }

    // Admins manage any event; organizers only the events they created.
    private void requireCanManage(Event event, String actorEmail) {
        User actor = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
        boolean owner = event.getOrganizer() != null
                && event.getOrganizer().getId().equals(actor.getId());
        if (actor.getRole() != UserRole.ADMIN && !(actor.getRole() == UserRole.ORGANIZER && owner)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You can only manage your own events");
        }
    }

    @Transactional(readOnly = true)
    public EventPoster getPoster(Long eventId) {
        return posterRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Poster not found"
                ));
    }

    private void storePoster(Event event, PosterFile file) {
        EventPoster poster = posterRepository.findById(event.getId())
                .orElseGet(EventPoster::new);
        poster.setEventId(event.getId());
        poster.setContentType(file.contentType());
        poster.setData(file.data());
        posterRepository.save(poster);
        event.setPosterUpdatedAt(LocalDateTime.now());
    }

    private PosterFile validatePoster(MultipartFile poster) {
        if (poster == null || poster.isEmpty()) {
            return null;
        }
        if (poster.getSize() > MAX_POSTER_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Poster must be at most 5 MB"
            );
        }

        byte[] data;
        try {
            data = poster.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Could not read the uploaded poster"
            );
        }

        String contentType = detectContentType(data);
        if (contentType == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Poster must be a JPEG, PNG or WEBP image"
            );
        }
        return new PosterFile(contentType, data);
    }

    // Sniff the real file signature instead of trusting the client-supplied Content-Type.
    private String detectContentType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "image/png";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private record PosterFile(String contentType, byte[] data) {
    }
}
