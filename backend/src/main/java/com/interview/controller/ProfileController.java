package com.interview.controller;

import com.interview.dto.ProfileDto;
import com.interview.model.User;
import com.interview.service.AuthService;
import com.interview.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(Authentication auth) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(profileService.getProfile(user.getId()));
    }

    @PutMapping
    public ResponseEntity<ProfileDto> updateProfile(Authentication auth, @RequestBody ProfileDto dto) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(profileService.updateProfile(user.getId(), dto));
    }

    @PostMapping("/resume")
    public ResponseEntity<ProfileDto> uploadResume(Authentication auth,
                                                    @RequestParam("file") MultipartFile file) throws Exception {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(profileService.uploadResume(user.getId(), file));
    }
}
