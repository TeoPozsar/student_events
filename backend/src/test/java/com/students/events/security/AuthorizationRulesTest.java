package com.students.events.security;

import com.students.events.event.EventController;
import com.students.events.event.EventService;
import com.students.events.registration.MyRegistrationsController;
import com.students.events.registration.RegistrationController;
import com.students.events.registration.RegistrationService;
import com.students.events.user.controller.UserController;
import com.students.events.user.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        EventController.class,
        RegistrationController.class,
        MyRegistrationsController.class,
        UserController.class
})
@Import(SecurityConfig.class)
class AuthorizationRulesTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean EventService eventService;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean ProfileService profileService;

    private static final String EVENT_JSON = """
            {"title":"T","description":"D","location":"L","category":"Technology",
             "startDateTime":"2999-01-01T10:00:00","endDateTime":"2999-01-01T12:00:00","capacity":5}""";

    @Test
    void anonymousIsRejectedOnProtectedEndpoints() throws Exception {
        mvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/events/1/register")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/events/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/registrations/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void posterIsPublic() throws Exception {
        com.students.events.event.EventPoster poster = new com.students.events.event.EventPoster();
        poster.setContentType("image/png");
        poster.setData(new byte[]{1});
        org.mockito.Mockito.when(eventService.getPoster(1L)).thenReturn(poster);
        mvc.perform(get("/api/events/1/poster")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotUseOrganizerOrAdminLists() throws Exception {
        mvc.perform(get("/api/events/mine")).andExpect(status().isForbidden());
        mvc.perform(get("/api/events/admin")).andExpect(status().isForbidden());
        mvc.perform(get("/api/events")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void organizerCanUseMineButNotAdminList() throws Exception {
        mvc.perform(get("/api/events/mine")).andExpect(status().isOk());
        mvc.perform(get("/api/events/admin")).andExpect(status().isForbidden());
        mvc.perform(get("/api/events")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUseAdminListButNotMine() throws Exception {
        mvc.perform(get("/api/events/admin")).andExpect(status().isOk());
        mvc.perform(get("/api/events/mine")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void endNotAfterStartIsRejected() throws Exception {
        String json = """
                {"title":"T","description":"D","location":"L","category":"Technology",
                 "startDateTime":"2999-01-01T10:00:00","endDateTime":"2999-01-01T10:00:00","capacity":5}""";
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void invalidCategoryIsRejected() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(EVENT_JSON.replace("Technology", "technology")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(EVENT_JSON.replace("Technology", "Sports")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void everySupportedCategoryIsAccepted() throws Exception {
        for (String c : new String[]{"Technology", "Social", "Workshops", "Volunteering"}) {
            mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                            .content(EVENT_JSON.replace("Technology", c)))
                    .andExpect(status().is2xxSuccessful());
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotManageEvents() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(EVENT_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/events")
                        .file(new MockMultipartFile("event", "", "application/json", EVENT_JSON.getBytes()))
                        .file(new MockMultipartFile("poster", "p.png", "image/png", new byte[]{1})))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/events/1").with(r -> { r.setMethod("PUT"); return r; })
                        .file(new MockMultipartFile("event", "", "application/json", EVENT_JSON.getBytes())))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/events/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void organizerCanManageButNotRegister() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(EVENT_JSON))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/events/1")).andExpect(status().isNoContent());
        mvc.perform(post("/api/events/1/register")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanManageButNotRegister() throws Exception {
        mvc.perform(delete("/api/events/1")).andExpect(status().isNoContent());
        mvc.perform(post("/api/events/1/register")).andExpect(status().isForbidden());
    }
}
