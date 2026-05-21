package com.gametrend.agent.livetrend.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("live_trend_platform_status")
public class LiveTrendPlatformStatus implements Persistable<Long> {

    @Id
    private Long id;

    @Transient
    @Builder.Default
    private boolean newEntity = false;

    private String platform;
    private boolean configured;
    private String status;
    private String message;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime lastFailureAt;
    private LocalDateTime updatedAt;

    @Override
    public boolean isNew() {
        return newEntity;
    }
}
