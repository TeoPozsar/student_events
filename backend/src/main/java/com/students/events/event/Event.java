package com.students.events.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.students.events.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "attendee_count", nullable = false)
    private Integer attendeeCount = 0;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @JsonIgnore
    @Column(name = "poster_updated_at")
    private LocalDateTime posterUpdatedAt;

    // Expose only the organizer's display name, never the full user record.
    @JsonProperty("organizer")
    public String getOrganizerName() {
        return organizer == null ? null : organizer.getFirstName() + " " + organizer.getLastName();
    }

    // Evaluated on every serialization, so it changes as time passes. Event times are
    // zone-less wall-clock values interpreted in the server's time zone (like @Future).
    @JsonProperty("status")
    public EventStatus getStatus() {
        if (startDateTime == null || endDateTime == null) {
            return null;
        }
        return EventStatus.of(startDateTime, endDateTime, LocalDateTime.now());
    }

    // The version query param busts browser caches when the poster is replaced.
    @JsonProperty("posterUrl")
    public String getPosterUrl() {
        if (posterUpdatedAt == null || id == null) {
            return null;
        }
        return "/api/events/" + id + "/poster?v=" + posterUpdatedAt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}