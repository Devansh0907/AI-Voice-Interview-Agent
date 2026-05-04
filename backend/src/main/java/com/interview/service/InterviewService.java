package com.interview.service;

import com.interview.dto.*;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.*;
import com.interview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final GeminiService geminiService;
    private final WhisperService whisperService;
    private final RagService ragService;

    /**
     * Start a new interview session.
     */
    public InterviewResponse startInterview(Long userId, InterviewStartRequest request) {
        Interview interview = Interview.builder()
                .user(User.builder().id(userId).build())
                .topic(request.getTopic() != null ? request.getTopic() : "General Software Engineering")
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : "MEDIUM")
                .maxQuestions(request.getMaxQuestions() != null ? request.getMaxQuestions() : 10)
                .status("IN_PROGRESS")
                .totalQuestions(0)
                .build();
        interview = interviewRepository.save(interview);

        // Generate first question using RAG context
        String context = ragService.hasProfile(userId)
                ? ragService.retrieveContext(userId, request.getTopic() != null ? request.getTopic() : "software engineering", 3)
                : "No profile information available.";

        String question = generateQuestion(context, new ArrayList<>(),
                interview.getTopic(), interview.getDifficulty(), 1);

        // Save question
        InterviewQuestion iq = InterviewQuestion.builder()
                .interview(interview)
                .questionText(question)
                .questionOrder(1)
                .build();
        questionRepository.save(iq);
        interview.setTotalQuestions(1);
        interviewRepository.save(interview);

        return toResponse(interview, List.of(iq), null);
    }

    /**
     * Submit an audio answer and get the next question.
     */
    public InterviewResponse submitAudioAnswer(Long interviewId, Long userId, MultipartFile audioFile) {
        Interview interview = getInterviewForUser(interviewId, userId);

        // Transcribe audio
        String transcript = whisperService.transcribe(audioFile);
        log.info("Transcribed answer: {}", transcript.substring(0, Math.min(100, transcript.length())));

        return processAnswer(interview, userId, transcript);
    }

    /**
     * Submit a text answer and get the next question.
     */
    public InterviewResponse submitTextAnswer(Long interviewId, Long userId, String answerText) {
        Interview interview = getInterviewForUser(interviewId, userId);
        return processAnswer(interview, userId, answerText);
    }

    /**
     * End an interview session.
     */
    public InterviewResponse endInterview(Long interviewId, Long userId) {
        Interview interview = getInterviewForUser(interviewId, userId);
        interview.setStatus("COMPLETED");
        interview.setEndedAt(java.time.LocalDateTime.now());
        interviewRepository.save(interview);

        List<InterviewQuestion> questions = questionRepository
                .findByInterviewIdOrderByQuestionOrderAsc(interviewId);

        return toResponse(interview, questions, null);
    }

    /**
     * Get all interviews for a user.
     */
    public List<InterviewResponse> getUserInterviews(Long userId) {
        List<Interview> interviews = interviewRepository.findByUserIdOrderByStartedAtDesc(userId);
        return interviews.stream().map(i -> {
            List<InterviewQuestion> questions = questionRepository
                    .findByInterviewIdOrderByQuestionOrderAsc(i.getId());
            return toResponse(i, questions, null);
        }).collect(Collectors.toList());
    }

    /**
     * Get a specific interview.
     */
    public InterviewResponse getInterview(Long interviewId, Long userId) {
        Interview interview = getInterviewForUser(interviewId, userId);
        List<InterviewQuestion> questions = questionRepository
                .findByInterviewIdOrderByQuestionOrderAsc(interviewId);
        return toResponse(interview, questions, null);
    }

    // --- Private helpers ---

    private InterviewResponse processAnswer(Interview interview, Long userId, String answerText) {
        List<InterviewQuestion> questions = questionRepository
                .findByInterviewIdOrderByQuestionOrderAsc(interview.getId());

        // Find the last unanswered question
        InterviewQuestion currentQuestion = questions.stream()
                .filter(q -> q.getAnswerText() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending question found"));

        // Save the answer
        currentQuestion.setAnswerText(answerText);

        // Evaluate the answer using Gemini
        String evalPrompt = String.format(
                "Evaluate this interview answer on a scale of 1-10 and provide brief feedback.\n\n" +
                "Question: %s\nAnswer: %s\n\n" +
                "Respond in this exact format:\nScore: [number]\nFeedback: [your feedback]",
                currentQuestion.getQuestionText(), answerText);

        try {
            String evaluation = geminiService.generateText(evalPrompt);
            // Parse score
            int score = parseScore(evaluation);
            currentQuestion.setScore(score);
            currentQuestion.setFeedback(evaluation);
        } catch (Exception e) {
            log.warn("Failed to evaluate answer: {}", e.getMessage());
            currentQuestion.setScore(5);
            currentQuestion.setFeedback("Evaluation unavailable");
        }

        questionRepository.save(currentQuestion);

        // Check if we should generate the next question
        boolean shouldContinue = questions.size() < interview.getMaxQuestions()
                && "IN_PROGRESS".equals(interview.getStatus());

        if (shouldContinue) {
            // Build conversation history
            List<Map<String, String>> history = new ArrayList<>();
            for (InterviewQuestion q : questions) {
                history.add(Map.of(
                        "question", q.getQuestionText(),
                        "answer", q.getAnswerText() != null ? q.getAnswerText() : ""
                ));
            }

            String context = ragService.hasProfile(userId)
                    ? ragService.retrieveContext(userId, interview.getTopic(), 3)
                    : "";

            String nextQuestion = generateQuestion(context, history,
                    interview.getTopic(), interview.getDifficulty(), questions.size() + 1);

            InterviewQuestion nextIq = InterviewQuestion.builder()
                    .interview(interview)
                    .questionText(nextQuestion)
                    .questionOrder(questions.size() + 1)
                    .build();
            questionRepository.save(nextIq);
            questions.add(nextIq);

            interview.setTotalQuestions(questions.size());
            interviewRepository.save(interview);
        }

        // Reload questions with updated data
        questions = questionRepository.findByInterviewIdOrderByQuestionOrderAsc(interview.getId());
        return toResponse(interview, questions, null);
    }

    private String generateQuestion(String context, List<Map<String, String>> history,
                                     String topic, String difficulty, int questionNumber) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert technical interviewer. ");
        prompt.append("Generate the next interview question.\n\n");

        if (!context.isEmpty()) {
            prompt.append("Candidate's profile/resume context:\n").append(context).append("\n\n");
        }

        if (!history.isEmpty()) {
            prompt.append("Previous conversation:\n");
            for (Map<String, String> qa : history) {
                prompt.append("Q: ").append(qa.get("question")).append("\n");
                prompt.append("A: ").append(qa.get("answer")).append("\n\n");
            }
        }

        prompt.append("Requirements:\n");
        prompt.append("- Topic: ").append(topic).append("\n");
        prompt.append("- Difficulty: ").append(difficulty).append("\n");
        prompt.append("- Question number: ").append(questionNumber).append("\n");
        prompt.append("- Be specific, practical, not generic\n");
        prompt.append("- If profile context is available, tailor the question to the candidate's experience\n");
        prompt.append("- Progressively increase difficulty\n");
        prompt.append("- Follow up on weak areas from previous answers if any\n\n");
        prompt.append("Respond with ONLY the question text, nothing else.");

        try {
            return geminiService.generateText(prompt.toString());
        } catch (Exception e) {
            log.error("Failed to generate question: {}", e.getMessage());
            return getDefaultQuestion(topic, questionNumber);
        }
    }

    private String getDefaultQuestion(String topic, int number) {
        List<String> defaults = List.of(
                "Tell me about your experience with " + topic + ".",
                "Can you explain a challenging project you worked on?",
                "How do you approach problem-solving in your work?",
                "What design patterns are you most familiar with?",
                "How do you handle code reviews and collaboration?",
                "Tell me about a time you debugged a complex issue.",
                "What's your approach to testing?",
                "How do you stay updated with new technologies?",
                "Describe your ideal development workflow.",
                "What are your technical goals for the next year?"
        );
        return defaults.get(Math.min(number - 1, defaults.size() - 1));
    }

    private int parseScore(String evaluation) {
        try {
            String[] lines = evaluation.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("score")) {
                    String numStr = line.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        int score = Integer.parseInt(numStr.substring(0, Math.min(2, numStr.length())));
                        return Math.min(10, Math.max(1, score));
                    }
                }
            }
        } catch (Exception ignored) {}
        return 5;
    }

    private Interview getInterviewForUser(Long interviewId, Long userId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        if (!interview.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to interview");
        }
        return interview;
    }

    private InterviewResponse toResponse(Interview i, List<InterviewQuestion> questions, FeedbackResponse fb) {
        List<QuestionResponse> qrs = questions.stream().map(q -> QuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .answerText(q.getAnswerText())
                .score(q.getScore())
                .feedback(q.getFeedback())
                .questionOrder(q.getQuestionOrder())
                .build()
        ).collect(Collectors.toList());

        return InterviewResponse.builder()
                .id(i.getId())
                .status(i.getStatus())
                .topic(i.getTopic())
                .difficulty(i.getDifficulty())
                .totalQuestions(i.getTotalQuestions())
                .maxQuestions(i.getMaxQuestions())
                .startedAt(i.getStartedAt())
                .endedAt(i.getEndedAt())
                .questions(qrs)
                .feedback(fb)
                .build();
    }
}
