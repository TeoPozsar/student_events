
package com.students.events.registration;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/register")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        return RegistrationResponse.from(registrationService.register(
                eventId,
                authentication.getName()
        ));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        registrationService.cancel(
                eventId,
                authentication.getName()
        );
    }
}
