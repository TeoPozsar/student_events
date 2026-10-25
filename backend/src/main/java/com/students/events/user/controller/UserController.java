package com.students.events.user.controller;

import com.students.events.user.User;
import com.students.events.user.dto.ProfileResponse;
import com.students.events.user.dto.UpdateProfileRequest;
import com.students.events.user.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse getCurrentUser(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @PutMapping("/me")
    public ProfileResponse updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        return profileService.updateProfile(authentication.getName(), request);
    }

    @PostMapping(value = "/me/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadPicture(
            @RequestPart("picture") MultipartFile picture,
            Authentication authentication
    ) {
        return profileService.updatePicture(authentication.getName(), picture);
    }

    @DeleteMapping("/me/picture")
    public ProfileResponse removePicture(Authentication authentication) {
        return profileService.removePicture(authentication.getName());
    }

    // Authenticated only: the frontend fetches it with the JWT and shows it through an object URL.
    @GetMapping("/me/picture")
    public ResponseEntity<byte[]> getPicture(Authentication authentication) {
        User user = profileService.getPictureOwner(authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(user.getProfilePictureContentType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(user.getProfilePicture());
    }
}
