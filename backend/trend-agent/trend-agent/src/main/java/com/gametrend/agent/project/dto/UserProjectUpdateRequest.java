package com.gametrend.agent.project.dto;

import com.gametrend.agent.project.entity.ProjectStatus;
import com.gametrend.agent.project.entity.ProjectType;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserProjectUpdateRequest(
        @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        ProjectType projectType,

        @Size(max = 200)
        String targetAudience,

        @Size(max = 100)
        String preferredPlatform,

        @Size(max = 10)
        List<@Size(max = 50) String> interactionFeatures,

        ProjectStatus status
) {
}
