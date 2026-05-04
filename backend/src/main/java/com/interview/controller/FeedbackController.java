package com.interview.controller;

import com.interview.dto.FeedbackResponse;
import com.interview.model.User;
import com.interview.service.AuthService;
import com.interview.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AuthService authService;

    @PostMapping("/{interviewId}")
    public ResponseEntity<FeedbackResponse> generateFeedback(
            Authentication auth, @PathVariable Long interviewId) {
        User user = authService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(feedbackService.generateFeedback(interviewId, user.getId()));
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<FeedbackResponse> getFeedback(@PathVariable Long interviewId) {
        return ResponseEntity.ok(feedbackService.getFeedback(interviewId));
    }
}
