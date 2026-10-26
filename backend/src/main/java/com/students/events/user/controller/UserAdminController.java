package com.students.events.user.controller;

import com.students.events.user.dto.UpdateUserRoleRequest;
import com.students.events.user.dto.UserSummaryResponse;
import com.students.events.user.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Restricted to ADMIN in SecurityConfig (/api/admin/**).
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<UserSummaryResponse> listUsers() {
        return userAdminService.listUsers();
    }

    @PutMapping("/{id}/role")
    public UserSummaryResponse updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        return userAdminService.updateRole(id, request.role(), authentication.getName());
    }
}
