package com.interview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.dto.FeedbackResponse;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.*;
import com.interview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Generate comprehensive feedback for a completed interview.
     */
    public FeedbackResponse generateFeedback(Long interviewId, Long userId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        // Check if feedback already exists
        return feedbackRepository.findByInterviewId(interviewId)
                .map(this::toResponse)
                .orElseGet(() -> createFeedback(interview));
    }

    private FeedbackResponse createFeedback(Interview interview) {
        List<InterviewQuestion> questions = questionRepository
                .findByInterviewIdOrderByQuestionOrderAsc(interview.getId());

        // Build transcript for Gemini
        StringBuilder transcript = new StringBuilder();
        transcript.append("Interview Topic: ").append(interview.getTopic()).append("\n");
        transcript.append("Difficulty: ").append(interview.getDifficulty()).append("\n\n");

        for (InterviewQuestion q : questions) {
            transcript.append("Q").append(q.getQuestionOrder()).append(": ").append(q.getQuestionText()).append("\n");
            transcript.append("A: ").append(q.getAnswerText() != null ? q.getAnswerText() : "No answer").append("\n\n");
        }

        String prompt = String.format("""
            Analyze this interview transcript and provide detailed feedback as JSON.
            
            %s
            
            Provide your analysis as a JSON object with these exact fields:
            {
              "overallScore": <number 0-100>,
              "technicalScore": <number 0-100>,
              "communicationScore": <number 0-100>,
              "problemSolvingScore": <number 0-100>,
              "strengths": "<comma-separated list of strengths>",
              "weaknesses": "<comma-separated list of weaknesses>",
              "overallFeedback": "<detailed paragraph of overall feedback>",
              "improvementAreas": "<comma-separated list of specific improvement areas>"
            }
            """, transcript.toString());

        Feedback feedback;
        try {
            String jsonResponse = geminiService.generateJsonResponse(prompt);
            JsonNode json = objectMapper.readTree(jsonResponse);

            feedback = Feedback.builder()
                    .interview(interview)
                    .overallScore(json.has("overallScore") ? json.get("overallScore").asInt() : 50)
                    .technicalScore(json.has("technicalScore") ? json.get("technicalScore").asInt() : 50)
                    .communicationScore(json.has("communicationScore") ? json.get("communicationScore").asInt() : 50)
                    .problemSolvingScore(json.has("problemSolvingScore") ? json.get("problemSolvingScore").asInt() : 50)
                    .strengths(json.has("strengths") ? json.get("strengths").asText() : "")
                    .weaknesses(json.has("weaknesses") ? json.get("weaknesses").asText() : "")
                    .overallFeedback(json.has("overallFeedback") ? json.get("overallFeedback").asText() : "")
                    .improvementAreas(json.has("improvementAreas") ? json.get("improvementAreas").asText() : "")
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini feedback: {}", e.getMessage());
            // Create default feedback based on question scores
            int avgScore = questions.stream()
                    .filter(q -> q.getScore() != null)
                    .mapToInt(InterviewQuestion::getScore)
                    .sum();
            int count = (int) questions.stream().filter(q -> q.getScore() != null).count();
            int avg = count > 0 ? (avgScore * 10) / count : 50;

            feedback = Feedback.builder()
                    .interview(interview)
                    .overallScore(avg)
                    .technicalScore(avg)
                    .communicationScore(avg)
                    .problemSolvingScore(avg)
                    .strengths("Completed the interview")
                    .weaknesses("Feedback generation failed - please retry")
                    .overallFeedback("Automated scoring based on per-question evaluation.")
                    .improvementAreas("Practice more and retry for detailed feedback")
                    .build();
        }

        feedback = feedbackRepository.save(feedback);

        // Mark interview as completed
        interview.setStatus("COMPLETED");
        interview.setEndedAt(java.time.LocalDateTime.now());
        interviewRepository.save(interview);

        return toResponse(feedback);
    }

    public FeedbackResponse getFeedback(Long interviewId) {
        Feedback feedback = feedbackRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
        return toResponse(feedback);
    }

    private FeedbackResponse toResponse(Feedback f) {
        return FeedbackResponse.builder()
                .id(f.getId())
                .overallScore(f.getOverallScore())
                .technicalScore(f.getTechnicalScore())
                .communicationScore(f.getCommunicationScore())
                .problemSolvingScore(f.getProblemSolvingScore())
                .strengths(f.getStrengths())
                .weaknesses(f.getWeaknesses())
                .overallFeedback(f.getOverallFeedback())
                .improvementAreas(f.getImprovementAreas())
                .build();
    }
}
