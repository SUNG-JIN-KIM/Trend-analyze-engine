package com.gametrend.agent.trend.exception;

public class TrendGameNotFoundException extends RuntimeException {

    public TrendGameNotFoundException(Long id) {
        super("트렌드 게임을 찾을 수 없습니다. id=%d".formatted(id));
    }
}
