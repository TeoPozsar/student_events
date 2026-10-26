
package com.students.events.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
        @Pattern(regexp = "Technology|Social|Workshops|Volunteering",
                message = "must be one of: Technology, Social, Workshops, Volunteering")
        String category,

        @NotNull
        LocalDateTime startDateTime,

        @NotNull
        LocalDateTime endDateTime,

        @NotNull
        @Min(1)
        Integer capacity
) {

    @JsonIgnore
    @AssertTrue(message = "must be after the start time")
    public boolean isEndDateTimeAfterStart() {
        return startDateTime == null || endDateTime == null || endDateTime.isAfter(startDateTime);
    }
}
