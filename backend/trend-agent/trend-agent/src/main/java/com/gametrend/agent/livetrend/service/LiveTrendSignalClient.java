package com.gametrend.agent.livetrend.service;

import java.util.List;

public interface LiveTrendSignalClient {

    String source();

    List<LiveGameSignal> fetchSignals();
}
