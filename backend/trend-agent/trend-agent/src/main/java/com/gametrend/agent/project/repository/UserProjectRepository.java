package com.gametrend.agent.project.repository;

import com.gametrend.agent.project.entity.UserProject;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserProjectRepository extends CrudRepository<UserProject, Long> {

    List<UserProject> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<UserProject> findByIdAndUserId(Long id, Long userId);
}
