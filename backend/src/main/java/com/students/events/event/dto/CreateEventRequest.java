
package com.students.events.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateEventRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        String description,

        @NotBlank
        @Size(max = 200)
        String location,

        @NotBlank
        @Size(max = 80)
        String category,

        @NotNull
        @Future(message = "must be in the future")
        LocalDateTime startDateTime,

        @NotNull
        @Min(1)
        Integer capacity
) {
}
