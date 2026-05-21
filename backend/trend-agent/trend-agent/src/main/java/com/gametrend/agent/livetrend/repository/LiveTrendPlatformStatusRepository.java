package com.gametrend.agent.livetrend.repository;

import com.gametrend.agent.livetrend.entity.LiveTrendPlatformStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LiveTrendPlatformStatusRepository extends CrudRepository<LiveTrendPlatformStatus, Long> {

    Optional<LiveTrendPlatformStatus> findByPlatform(String platform);
}
