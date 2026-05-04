package com.interview.controller;

import com.interview.dto.*;
import com.interview.model.User;
import com.interview.service.AuthService;
import com.interview.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final AuthService authService;

    @PostMapping("/start")
    public ResponseEntity<InterviewResponse> startInterview(
            Authentication auth, @RequestBody InterviewStartRequest request) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(interviewService.startInterview(user.getId(), request));
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<InterviewResponse> submitAudioAnswer(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam("audio") MultipartFile audioFile) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(interviewService.submitAudioAnswer(id, user.getId(), audioFile));
    }

    @PostMapping("/{id}/answer-text")
    public ResponseEntity<InterviewResponse> submitTextAnswer(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(interviewService.submitTextAnswer(id, user.getId(), body.get("answer")));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<InterviewResponse> endInterview(
            Authentication auth, @PathVariable Long id) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(interviewService.endInterview(id, user.getId()));
    }

    @GetMapping
    public ResponseEntity<List<InterviewResponse>> getInterviews(Authentication auth) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(interviewService.getUserInterviews(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterview(
            Authentication auth, @PathVariable Long id) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(interviewService.getInterview(id, user.getId()));
    }
}
