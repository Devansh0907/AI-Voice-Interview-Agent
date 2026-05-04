package com.interview.dto;

import lombok.Data;

@Data
public class ProfileDto {
    private Long id;
    private String title;
    private Integer experienceYears;
    private String skills;
    private String education;
    private String resumeText;
    private boolean hasResume;
}
