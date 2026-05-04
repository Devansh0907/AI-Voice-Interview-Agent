package com.interview.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InterviewResponse {
    private Long id;
    private String status;
    private String topic;
    private String difficulty;
    private Integer totalQuestions;
    private Integer maxQuestions;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<QuestionResponse> questions;
    private FeedbackResponse feedback;
}
