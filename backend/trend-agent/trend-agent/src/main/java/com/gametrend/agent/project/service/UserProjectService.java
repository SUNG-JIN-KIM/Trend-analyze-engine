package com.gametrend.agent.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.project.dto.UserProjectCreateRequest;
import com.gametrend.agent.project.dto.UserProjectResponse;
import com.gametrend.agent.project.dto.UserProjectUpdateRequest;
import com.gametrend.agent.project.entity.ProjectStatus;
import com.gametrend.agent.project.entity.UserProject;
import com.gametrend.agent.project.exception.UserProjectNotFoundException;
import com.gametrend.agent.project.repository.UserProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProjectService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final UserProjectRepository repository;
    private final ObjectMapper objectMapper;

    public UserProjectResponse create(Long userId, UserProjectCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        UserProject project = repository.save(UserProject.builder()
                .userId(userId)
                .title(request.title().strip())
                .description(stripToNull(request.description()))
                .projectType(request.projectType())
                .targetAudience(stripToNull(request.targetAudience()))
                .preferredPlatform(stripToNull(request.preferredPlatform()))
                .interactionFeaturesJson(writeList(request.interactionFeatures()))
                .status(ProjectStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toResponse(project);
    }

    public List<UserProjectResponse> findProjects(Long userId) {
        return repository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserProjectResponse findProject(Long projectId, Long userId) {
        return toResponse(findOwnedProject(projectId, userId));
    }

    public UserProjectResponse update(Long projectId, Long userId, UserProjectUpdateRequest request) {
        UserProject project = findOwnedProject(projectId, userId);
        LocalDateTime now = LocalDateTime.now();
        UserProject updated = repository.save(project.toBuilder()
                .title(resolveText(request.title(), project.getTitle(), true))
                .description(resolveText(request.description(), project.getDescription(), false))
                .projectType(request.projectType() == null ? project.getProjectType() : request.projectType())
                .targetAudience(resolveText(request.targetAudience(), project.getTargetAudience(), false))
                .preferredPlatform(resolveText(request.preferredPlatform(), project.getPreferredPlatform(), false))
                .interactionFeaturesJson(request.interactionFeatures() == null
                        ? project.getInteractionFeaturesJson()
                        : writeList(request.interactionFeatures()))
                .status(request.status() == null ? project.getStatus() : request.status())
                .updatedAt(now)
                .build());
        return toResponse(updated);
    }

    public void archive(Long projectId, Long userId) {
        UserProject project = findOwnedProject(projectId, userId);
        repository.save(project.toBuilder()
                .status(ProjectStatus.ARCHIVED)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    public UserProject validateOwnedProject(Long projectId, Long userId) {
        return findOwnedProject(projectId, userId);
    }

    public void touch(Long projectId, Long userId) {
        UserProject project = findOwnedProject(projectId, userId);
        repository.save(project.toBuilder()
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private UserProject findOwnedProject(Long projectId, Long userId) {
        return repository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new UserProjectNotFoundException(projectId));
    }

    private UserProjectResponse toResponse(UserProject project) {
        return UserProjectResponse.from(project, readList(project.getInteractionFeaturesJson()));
    }

    private String resolveText(String candidate, String previous, boolean required) {
        if (candidate == null) {
            return previous;
        }
        if (candidate.isBlank()) {
            return required ? previous : null;
        }
        return candidate.strip();
    }

    private String stripToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private List<String> readList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::strip)
                    .toList());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("프로젝트 상호작용 기능 JSON 직렬화에 실패했습니다.", ex);
        }
    }
}
