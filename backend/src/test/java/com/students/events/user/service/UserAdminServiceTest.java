package com.students.events.user.service;

import com.students.events.security.JwtService;
import com.students.events.user.User;
import com.students.events.user.UserRole;
import com.students.events.user.dto.RegisterRequest;
import com.students.events.user.dto.UserSummaryResponse;
import com.students.events.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAdminServiceTest {

    UserRepository users = mock(UserRepository.class);
    UserAdminService service = new UserAdminService(users);

    private User user(long id, String email, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setFirstName("F" + id);
        u.setLastName("L" + id);
        u.setEmail(email);
        u.setPassword("hash-" + id);
        u.setRole(role);
        when(users.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private int statusOf(Runnable call) {
        return assertThrows(ResponseStatusException.class, call::run).getStatusCode().value();
    }

    @Test
    void changesRoleOfAnotherUserAndKeepsOtherData() {
        User target = user(2, "u@x.ro", UserRole.USER);
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserSummaryResponse result = service.updateRole(2L, "organizer", "admin@x.ro");

        assertEquals("ORGANIZER", result.role());
        assertEquals("hash-2", target.getPassword());
        assertEquals("u@x.ro", result.email());
    }

    @Test
    void adminCannotChangeOwnRoleEvenWithDifferentEmailCase() {
        user(1, "Admin@x.ro", UserRole.ADMIN);

        assertEquals(403, statusOf(() -> service.updateRole(1L, "USER", "admin@X.ro")));
        verify(users, never()).save(any());
    }

    @Test
    void invalidRoleIsRejected() {
        user(2, "u@x.ro", UserRole.USER);

        assertEquals(400, statusOf(() -> service.updateRole(2L, "SUPERUSER", "admin@x.ro")));
        assertEquals(400, statusOf(() -> service.updateRole(2L, null, "admin@x.ro")));
        verify(users, never()).save(any());
    }

    @Test
    void unknownUserIsNotFound() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertEquals(404, statusOf(() -> service.updateRole(99L, "USER", "admin@x.ro")));
    }

    @Test
    void listReturnsOnlySafeFields() {
        User u = new User();
        u.setId(3L);
        u.setFirstName("Ana");
        u.setLastName("Pop");
        u.setEmail("ana@x.ro");
        u.setPassword("secret-hash");
        u.setRole(UserRole.USER);
        when(users.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(u));

        List<UserSummaryResponse> result = service.listUsers();

        assertEquals(1, result.size());
        assertFalse(result.get(0).toString().contains("secret-hash"));
    }

    @Test
    void publicRegistrationAlwaysCreatesUserRole() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserRepository repo = mock(UserRepository.class);
        when(encoder.encode(any())).thenReturn("hash");
        when(repo.save(any(User.class))).thenAnswer(i -> {
            User saved = i.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        AuthService auth = new AuthService(repo, encoder, mock(AuthenticationManager.class), mock(JwtService.class));

        var response = auth.register(new RegisterRequest("A", "B", "a@b.ro", "password123", null, null));

        assertEquals("USER", response.role());
    }
}
