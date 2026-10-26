package com.students.events.user.service;

import com.students.events.user.User;
import com.students.events.user.UserRole;
import com.students.events.user.dto.UserSummaryResponse;
import com.students.events.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserAdminService {

    private final UserRepository userRepository;

    public UserAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers() {
        return userRepository
                .findAll(Sort.by("firstName", "lastName", "id"))
                .stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    // The acting admin comes from the authenticated principal, never from the request.
    @Transactional
    public UserSummaryResponse updateRole(Long userId, String requestedRole, String adminEmail) {
        UserRole role = parseRole(requestedRole);

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (target.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You cannot change your own role");
        }

        target.setRole(role);
        return UserSummaryResponse.from(userRepository.save(target));
    }

    private UserRole parseRole(String value) {
        try {
            return UserRole.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be one of USER, ORGANIZER, ADMIN");
        }
    }
}
