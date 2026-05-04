package com.interview.service;

import com.interview.dto.ProfileDto;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.Profile;
import com.interview.model.User;
import com.interview.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final RagService ragService;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    public ProfileDto getProfile(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        return toDto(profile);
    }

    public ProfileDto updateProfile(Long userId, ProfileDto dto) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        profile.setTitle(dto.getTitle());
        profile.setExperienceYears(dto.getExperienceYears());
        profile.setSkills(dto.getSkills());
        profile.setEducation(dto.getEducation());

        profile = profileRepository.save(profile);

        // Re-ingest profile into RAG vector store
        ingestToRag(userId, profile);

        return toDto(profile);
    }

    public ProfileDto uploadResume(Long userId, MultipartFile file) throws IOException {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Create upload directory
        Path uploadPath = Paths.get(uploadDir, "resumes");
        Files.createDirectories(uploadPath);

        // Save file
        String filename = userId + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        profile.setResumeFilePath(filePath.toString());

        // Extract text from PDF or plain text
        String resumeText = extractText(file);
        profile.setResumeText(resumeText);

        profile = profileRepository.save(profile);

        // Ingest into RAG
        ingestToRag(userId, profile);

        log.info("Resume uploaded for user {}: {} chars extracted", userId, resumeText.length());
        return toDto(profile);
    }

    private String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(doc);
            }
        }
        // Plain text fallback
        return new String(file.getBytes());
    }

    private void ingestToRag(Long userId, Profile profile) {
        StringBuilder profileText = new StringBuilder();
        if (profile.getTitle() != null) profileText.append("Title: ").append(profile.getTitle()).append("\n");
        if (profile.getExperienceYears() != null) profileText.append("Experience: ").append(profile.getExperienceYears()).append(" years\n");
        if (profile.getSkills() != null) profileText.append("Skills: ").append(profile.getSkills()).append("\n");
        if (profile.getEducation() != null) profileText.append("Education: ").append(profile.getEducation()).append("\n");
        if (profile.getResumeText() != null) profileText.append("\nResume:\n").append(profile.getResumeText());

        if (profileText.length() > 0) {
            ragService.ingestProfile(userId, profileText.toString());
        }
    }

    private ProfileDto toDto(Profile profile) {
        ProfileDto dto = new ProfileDto();
        dto.setId(profile.getId());
        dto.setTitle(profile.getTitle());
        dto.setExperienceYears(profile.getExperienceYears());
        dto.setSkills(profile.getSkills());
        dto.setEducation(profile.getEducation());
        dto.setResumeText(profile.getResumeText());
        dto.setHasResume(profile.getResumeFilePath() != null);
        return dto;
    }
}
