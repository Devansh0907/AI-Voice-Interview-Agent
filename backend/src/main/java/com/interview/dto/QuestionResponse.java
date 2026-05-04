package com.interview.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionResponse {
    private Long id;
    private String questionText;
    private String answerText;
    private Integer score;
    private String feedback;
    private Integer questionOrder;
}
