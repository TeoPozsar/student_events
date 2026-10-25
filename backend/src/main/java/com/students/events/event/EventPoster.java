package com.students.events.event;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_posters")
@Getter
@Setter
@NoArgsConstructor
public class EventPoster {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] data;
}
