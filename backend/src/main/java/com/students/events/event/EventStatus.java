package com.students.events.event;

import java.time.LocalDateTime;

/** Derived from the event's start/end times; never stored. */
public enum EventStatus {
    UPCOMING,
    IN_PROGRESS,
    FINISHED;

    public static EventStatus of(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (now.isBefore(start)) {
            return UPCOMING;
        }
        if (now.isBefore(end)) {
            return IN_PROGRESS;
        }
        return FINISHED;
    }
}
