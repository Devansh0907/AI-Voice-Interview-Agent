package com.interview.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackResponse {
    private Long id;
    private Integer overallScore;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer problemSolvingScore;
    private String strengths;
    private String weaknesses;
    private String overallFeedback;
    private String improvementAreas;
}
