package com.gametrend.agent.project.dto;

import com.gametrend.agent.project.entity.ProjectStatus;
import com.gametrend.agent.project.entity.ProjectType;
import com.gametrend.agent.project.entity.UserProject;

import java.time.LocalDateTime;
import java.util.List;

public record UserProjectResponse(
        Long id,
        String title,
        String description,
        ProjectType projectType,
        String targetAudience,
        String preferredPlatform,
        List<String> interactionFeatures,
        ProjectStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static UserProjectResponse from(UserProject project, List<String> interactionFeatures) {
        return new UserProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getProjectType(),
                project.getTargetAudience(),
                project.getPreferredPlatform(),
                interactionFeatures,
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
