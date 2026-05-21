package com.gametrend.agent.project.exception;

public class UserProjectNotFoundException extends RuntimeException {

    public UserProjectNotFoundException(Long projectId) {
        super("프로젝트를 찾을 수 없습니다. projectId=" + projectId);
    }
}
