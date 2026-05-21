package com.gametrend.agent.gameimage;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class GameImageResolver {

    public static final String PLACEHOLDER_IMAGE_URL =
            "https://placehold.co/640x360/111827/f9738a?text=Game+Trend";

    private static final Map<String, String> SEED_IMAGE_URLS = Map.ofEntries(
            Map.entry(normalizeKey("Counter-Strike 2"), steamHeaderImageUrl(730)),
            Map.entry(normalizeKey("CS2"), steamHeaderImageUrl(730)),
            Map.entry(normalizeKey("League of Legends"), placeholderImageUrl("League of Legends")),
            Map.entry(normalizeKey("LoL"), placeholderImageUrl("League of Legends")),
            Map.entry(normalizeKey("PUBG"), steamHeaderImageUrl(578080)),
            Map.entry(normalizeKey("PLAYERUNKNOWN'S BATTLEGROUNDS"), steamHeaderImageUrl(578080)),
            Map.entry(normalizeKey("배틀그라운드"), steamHeaderImageUrl(578080)),
            Map.entry(normalizeKey("Minecraft"), placeholderImageUrl("Minecraft")),
            Map.entry(normalizeKey("Grand Theft Auto V"), steamHeaderImageUrl(271590)),
            Map.entry(normalizeKey("GTA V"), steamHeaderImageUrl(271590)),
            Map.entry(normalizeKey("Subnautica 2"), steamHeaderImageUrl(1962700)),
            Map.entry(normalizeKey("Garry's Mod"), steamHeaderImageUrl(4000)),
            Map.entry(normalizeKey("Phasmophobia"), steamHeaderImageUrl(739630)),
            Map.entry(normalizeKey("Among Us"), steamHeaderImageUrl(945360)),
            Map.entry(normalizeKey("Keep Talking and Nobody Explodes"), steamHeaderImageUrl(341800)),
            Map.entry(normalizeKey("Papers, Please"), steamHeaderImageUrl(239030)),
            Map.entry(normalizeKey("Vampire Survivors"), steamHeaderImageUrl(1794680)),
            Map.entry(normalizeKey("Slay the Spire"), steamHeaderImageUrl(646570)),
            Map.entry(normalizeKey("The Stanley Parable"), steamHeaderImageUrl(221910)),
            Map.entry(normalizeKey("Don't Starve Together"), steamHeaderImageUrl(322330)),
            Map.entry(normalizeKey("Project Zomboid"), steamHeaderImageUrl(108600))
    );

    public String resolve(String title, String sourceKeyword, Integer steamAppId) {
        return resolveImageUrl(title, sourceKeyword, steamAppId);
    }

    public static String resolveImageUrl(String title, String sourceKeyword, Integer steamAppId) {
        return findSeedImageUrl(title)
                .or(() -> findSeedImageUrl(sourceKeyword))
                .or(() -> steamAppId == null || steamAppId <= 0
                        ? Optional.empty()
                        : Optional.of(steamHeaderImageUrl(steamAppId)))
                .orElse(PLACEHOLDER_IMAGE_URL);
    }

    public static String placeholderImageUrl(String label) {
        String text = label == null || label.isBlank() ? "Game Trend" : label.strip();
        return "https://placehold.co/640x360/111827/f9738a?text="
                + URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    public static String steamHeaderImageUrl(Integer steamAppId) {
        if (steamAppId == null || steamAppId <= 0) {
            return PLACEHOLDER_IMAGE_URL;
        }
        return steamHeaderImageUrl(steamAppId.intValue());
    }

    private static String steamHeaderImageUrl(int steamAppId) {
        return "https://cdn.akamai.steamstatic.com/steam/apps/%d/header.jpg".formatted(steamAppId);
    }

    private static Optional<String> findSeedImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String exactKey = normalizeKey(value);
        String exactMatch = SEED_IMAGE_URLS.get(exactKey);
        if (exactMatch != null) {
            return Optional.of(exactMatch);
        }
        return SEED_IMAGE_URLS.entrySet()
                .stream()
                .filter(entry -> exactKey.contains(entry.getKey()) || entry.getKey().contains(exactKey))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣]+", "")
                .strip();
    }
}
