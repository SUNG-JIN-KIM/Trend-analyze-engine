package com.gametrend.agent.livetrend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "live-trends")
public class LiveTrendProperties {

    private boolean schedulerEnabled = true;
    private long refreshIntervalMs = 1_800_000L;
    private boolean refreshOnStartup = false;
    private boolean exposeFallbackData = false;
    private Platforms platforms = new Platforms();

    @Getter
    @Setter
    public static class Platforms {

        private PlatformCredential twitch = new PlatformCredential();
        private PlatformCredential chzzk = new PlatformCredential();
        private PlatformCredential soop = new PlatformCredential();
        private SteamPlatform steam = new SteamPlatform();
    }

    @Getter
    @Setter
    public static class PlatformCredential {

        private String clientId;
        private String clientSecret;
        private String accessToken;
        private String refreshToken;
        private String apiBaseUrl;
        private String redirectUri;
        private String apiKey;
    }

    @Getter
    @Setter
    public static class SteamPlatform {

        private boolean usePublicApi = true;
        private String apiKey;
    }
}
