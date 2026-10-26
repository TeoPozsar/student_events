package com.students.events.security;

import com.students.events.event.Event;
import com.students.events.registration.EventRegistration;
import com.students.events.registration.MyRegistrationsController;
import com.students.events.registration.RegistrationController;
import com.students.events.registration.RegistrationService;
import com.students.events.registration.RegistrationStatus;
import com.students.events.user.controller.UserAdminController;
import com.students.events.user.dto.UserSummaryResponse;
import com.students.events.user.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserAdminController.class,
        RegistrationController.class,
        MyRegistrationsController.class
})
@Import(SecurityConfig.class)
class UserAdminSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean UserAdminService userAdminService;
    @MockitoBean RegistrationService registrationService;

    private static final String ROLE_JSON = "{\"role\":\"ORGANIZER\"}";

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/users/2/role").contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void userCannotListOrChangeRoles() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/2/role").contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isForbidden());
        verify(userAdminService, never()).updateRole(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"ORGANIZER"})
    void organizerCannotListOrChangeRoles() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/2/role").contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isForbidden());
        verify(userAdminService, never()).updateRole(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin@x.ro", roles = {"ADMIN"})
    void adminCanListUsersWithoutSensitiveFields() throws Exception {
        when(userAdminService.listUsers()).thenReturn(
                List.of(new UserSummaryResponse(1L, "Ana", "Pop", "ana@x.ro", "USER")));

        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("ana@x.ro"))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@x.ro", roles = {"ADMIN"})
    void adminCanChangeRoleUsingIdentityFromSecurityContext() throws Exception {
        when(userAdminService.updateRole(eq(2L), eq("ORGANIZER"), eq("admin@x.ro")))
                .thenReturn(new UserSummaryResponse(2L, "Ana", "Pop", "ana@x.ro", "ORGANIZER"));

        mvc.perform(put("/api/admin/users/2/role").contentType(MediaType.APPLICATION_JSON).content(ROLE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void blankRoleIsRejected() throws Exception {
        mvc.perform(put("/api/admin/users/2/role").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void adminCannotRegisterCancelOrListRegistrations() throws Exception {
        mvc.perform(post("/api/events/1/register")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/events/1/register")).andExpect(status().isForbidden());
        mvc.perform(get("/api/registrations/me")).andExpect(status().isForbidden());
        verify(registrationService, never()).register(any(), any());
        verify(registrationService, never()).cancel(any(), any());
    }

    @Test
    @WithMockUser(username = "user@x.ro", roles = {"USER"})
    void userCanStillRegisterAndCancel() throws Exception {
        Event event = new Event();
        event.setId(1L);
        EventRegistration registration = new EventRegistration();
        registration.setId(9L);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(LocalDateTime.now());
        when(registrationService.register(1L, "user@x.ro")).thenReturn(registration);

        mvc.perform(post("/api/events/1/register")).andExpect(status().isCreated());
        mvc.perform(delete("/api/events/1/register")).andExpect(status().isNoContent());
        mvc.perform(get("/api/registrations/me")).andExpect(status().isOk());
    }
}
