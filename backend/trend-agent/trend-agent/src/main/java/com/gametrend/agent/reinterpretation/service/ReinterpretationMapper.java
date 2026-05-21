package com.gametrend.agent.reinterpretation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.reinterpretation.dto.LegacyGameResponse;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationCandidateResponse;
import com.gametrend.agent.reinterpretation.entity.GameReinterpretationCandidate;
import com.gametrend.agent.reinterpretation.entity.LegacyGame;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReinterpretationMapper {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ReinterpretationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("목록 JSON 직렬화에 실패했습니다.", ex);
        }
    }

    List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    LegacyGameResponse toLegacyGameResponse(LegacyGame game) {
        return new LegacyGameResponse(
                game.getId(),
                game.getTitle(),
                game.getSource(),
                game.getSourceGameId(),
                game.getSteamAppId(),
                game.getReleaseYear(),
                readList(game.getGenresJson()),
                readList(game.getTagsJson()),
                readList(game.getMechanicsJson()),
                readList(game.getInteractionHintsJson()),
                game.getDevFeasibilityScore(),
                game.getReviewCount(),
                game.getPositiveReviewRate(),
                game.getLegacyPopularityScore(),
                game.getReviewSentimentScore(),
                game.getDataOrigin(),
                game.getReason(),
                game.getUpdatedAt()
        );
    }

    ReinterpretationCandidateResponse toCandidateResponse(GameReinterpretationCandidate candidate) {
        return new ReinterpretationCandidateResponse(
                candidate.getId(),
                candidate.getLegacyGameId(),
                candidate.getTitle(),
                candidate.getSource(),
                candidate.getSourceGameId(),
                candidate.getSteamAppId(),
                candidate.getReleaseYear(),
                readList(candidate.getGenresJson()),
                readList(candidate.getTagsJson()),
                readList(candidate.getMechanicsJson()),
                readList(candidate.getInteractionHintsJson()),
                candidate.getLegacyPopularityScore(),
                candidate.getReviewSentimentScore(),
                candidate.getMechanicUniquenessScore(),
                candidate.getStreamabilityScore(),
                candidate.getInteractionFitScore(),
                candidate.getModernTrendFitScore(),
                candidate.getDevFeasibilityScore(),
                candidate.getReinterpretationScore(),
                candidate.getReinterpretationConcept(),
                candidate.getReason(),
                candidate.getDataOrigin(),
                candidate.getReviewCount(),
                candidate.getPositiveReviewRate(),
                readList(candidate.getMatchedLiveTrendSourcesJson()),
                candidate.getUpdatedAt()
        );
    }
}
