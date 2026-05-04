package com.interview.dto;

import lombok.Data;

@Data
public class InterviewStartRequest {
    private String topic;
    private String difficulty; // EASY, MEDIUM, HARD
    private Integer maxQuestions; // default 10
}
