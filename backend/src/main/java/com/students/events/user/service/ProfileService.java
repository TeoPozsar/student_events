package com.students.events.user.service;

import com.students.events.user.User;
import com.students.events.user.dto.ProfileResponse;
import com.students.events.user.dto.UpdateProfileRequest;
import com.students.events.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
public class ProfileService {

    private static final long MAX_PICTURE_BYTES = 5L * 1024 * 1024;

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        return ProfileResponse.from(findUser(email));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUser(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setUniversity(blankToNull(request.university()));
        user.setLocation(blankToNull(request.location()));
        return ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public ProfileResponse updatePicture(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No picture was uploaded");
        }
        if (file.getSize() > MAX_PICTURE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Picture must be at most 5 MB");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded picture");
        }

        String contentType = detectContentType(data);
        if (contentType == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Picture must be a JPEG, PNG or WEBP image"
            );
        }

        User user = findUser(email);
        user.setProfilePicture(data);
        user.setProfilePictureContentType(contentType);
        return ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public ProfileResponse removePicture(String email) {
        User user = findUser(email);
        user.setProfilePicture(null);
        user.setProfilePictureContentType(null);
        return ProfileResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public User getPictureOwner(String email) {
        User user = findUser(email);
        if (user.getProfilePicture() == null || user.getProfilePicture().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile picture");
        }
        return user;
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Sniff the real file signature instead of trusting the client-supplied Content-Type.
    private String detectContentType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "image/png";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
