package com.interview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG (Retrieval-Augmented Generation) Service.
 * Uses in-memory vector store with cosine similarity for development.
 * Stores resume/profile chunks as embeddings and retrieves relevant context.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final GeminiService geminiService;

    // In-memory vector store: userId -> list of (chunk text, embedding)
    private final Map<Long, List<DocumentChunk>> vectorStore = new ConcurrentHashMap<>();

    /**
     * Ingest a user's profile/resume text into the vector store.
     * Splits text into chunks and generates embeddings for each.
     */
    public void ingestProfile(Long userId, String profileText) {
        log.info("Ingesting profile for user {}", userId);
        List<String> chunks = splitIntoChunks(profileText, 500, 50);
        List<DocumentChunk> documentChunks = new ArrayList<>();

        for (String chunk : chunks) {
            try {
                List<Float> embedding = geminiService.generateEmbedding(chunk);
                documentChunks.add(new DocumentChunk(chunk, embedding));
            } catch (Exception e) {
                log.warn("Failed to embed chunk: {}", e.getMessage());
                // Store chunk without embedding as fallback
                documentChunks.add(new DocumentChunk(chunk, Collections.emptyList()));
            }
        }

        vectorStore.put(userId, documentChunks);
        log.info("Ingested {} chunks for user {}", documentChunks.size(), userId);
    }

    /**
     * Retrieve the most relevant context chunks for a given query.
     */
    public String retrieveContext(Long userId, String query, int topK) {
        List<DocumentChunk> chunks = vectorStore.get(userId);
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        try {
            List<Float> queryEmbedding = geminiService.generateEmbedding(query);

            // Score each chunk by cosine similarity
            List<ScoredChunk> scored = new ArrayList<>();
            for (DocumentChunk chunk : chunks) {
                if (!chunk.embedding.isEmpty()) {
                    double similarity = cosineSimilarity(queryEmbedding, chunk.embedding);
                    scored.add(new ScoredChunk(chunk.text, similarity));
                } else {
                    // Fallback: basic keyword matching
                    double keywordScore = keywordSimilarity(query, chunk.text);
                    scored.add(new ScoredChunk(chunk.text, keywordScore));
                }
            }

            // Sort by score descending and take top K
            scored.sort((a, b) -> Double.compare(b.score, a.score));
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < Math.min(topK, scored.size()); i++) {
                context.append(scored.get(i).text).append("\n\n");
            }

            return context.toString().trim();
        } catch (Exception e) {
            log.warn("Vector search failed, returning all chunks: {}", e.getMessage());
            // Fallback: return all chunks concatenated
            StringBuilder sb = new StringBuilder();
            for (DocumentChunk chunk : chunks) {
                sb.append(chunk.text).append("\n\n");
            }
            return sb.toString().trim();
        }
    }

    /**
     * Get all stored context for a user (for simple retrieval without embedding).
     */
    public String getAllContext(Long userId) {
        List<DocumentChunk> chunks = vectorStore.get(userId);
        if (chunks == null || chunks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            sb.append(chunk.text).append("\n\n");
        }
        return sb.toString().trim();
    }

    /**
     * Check if a user has ingested profile data.
     */
    public boolean hasProfile(Long userId) {
        return vectorStore.containsKey(userId) && !vectorStore.get(userId).isEmpty();
    }

    // --- Utility Methods ---

    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i += (chunkSize - overlap)) {
            int end = Math.min(i + chunkSize, words.length);
            StringBuilder chunk = new StringBuilder();
            for (int j = i; j < end; j++) {
                chunk.append(words[j]).append(" ");
            }
            String trimmed = chunk.toString().trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
            if (end >= words.length) break;
        }
        return chunks;
    }

    private double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a.size() != b.size() || a.isEmpty()) return 0.0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        return (normA == 0 || normB == 0) ? 0.0 : dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double keywordSimilarity(String query, String text) {
        String[] queryWords = query.toLowerCase().split("\\s+");
        String lowerText = text.toLowerCase();
        int matches = 0;
        for (String word : queryWords) {
            if (lowerText.contains(word)) matches++;
        }
        return queryWords.length > 0 ? (double) matches / queryWords.length : 0;
    }

    // Inner classes
    private record DocumentChunk(String text, List<Float> embedding) {}
    private record ScoredChunk(String text, double score) {}
}
