package com.students.events.registration;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class MyRegistrationsController {

    private final RegistrationService registrationService;

    public MyRegistrationsController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    // The user is always taken from the JWT; no user id is accepted from the client.
    @GetMapping("/me")
    public List<RegistrationResponse> myRegistrations(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return registrationService.findActiveForUser(authentication.getName());
    }
}
