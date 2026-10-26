
package com.students.events.event;

import com.students.events.event.dto.CreateEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // Discovery: only events that have not finished.
    @GetMapping
    public List<Event> getDiscoverableEvents() {
        return eventService.getDiscoverableEvents();
    }

    // ORGANIZER only (SecurityConfig): the organizer is taken from the JWT.
    @GetMapping("/mine")
    public List<Event> getMyEvents(Authentication authentication) {
        return eventService.getOrganizerEvents(authentication.getName());
    }

    // ADMIN only (SecurityConfig): every event in every status.
    @GetMapping("/admin")
    public List<Event> getAllEventsForAdmin(Authentication authentication) {
        return eventService.getAllEventsForAdmin(authentication.getName());
    }

    @GetMapping("/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Event createEvent(
            @Valid @RequestPart("event") CreateEventRequest request,
            @RequestPart(value = "poster", required = false) MultipartFile poster,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return eventService.createEvent(
                request,
                authentication.getName(),
                poster
        );
    }

    // Plain JSON create (no poster), for clients that do not send multipart.
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Event createEventJson(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication
    ) {
        return createEvent(request, null, authentication);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Event updateEvent(
            @PathVariable Long id,
            @Valid @RequestPart("event") CreateEventRequest request,
            @RequestPart(value = "poster", required = false) MultipartFile poster,
            @RequestParam(value = "removePoster", defaultValue = "false") boolean removePoster,
            Authentication authentication
    ) {
        return eventService.updateEvent(id, request, poster, removePoster, authentication.getName());
    }

    @GetMapping("/{id}/poster")
    public ResponseEntity<byte[]> getPoster(@PathVariable Long id) {
        EventPoster poster = eventService.getPoster(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(poster.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(poster.getData());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long id, Authentication authentication) {
        eventService.deleteEvent(id, authentication.getName());
    }
}
