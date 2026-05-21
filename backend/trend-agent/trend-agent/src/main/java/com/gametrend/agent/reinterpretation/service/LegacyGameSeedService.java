package com.gametrend.agent.reinterpretation.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LegacyGameSeedService {

    public List<LegacyGameSeed> seeds() {
        return List.of(
                new LegacyGameSeed("Phasmophobia", "STEAM", "739630", 739630, 2020,
                        List.of("Horror", "Co-op"), List.of("horror", "ghost", "voice", "co-op"),
                        List.of("voice recognition", "co-op investigation", "reaction horror"),
                        List.of("WEBCAM", "STT"), 92, 95, 96, 72, 620_000, 0.96),
                new LegacyGameSeed("Among Us", "STEAM", "945360", 945360, 2018,
                        List.of("Social Deduction", "Party"), List.of("deduction", "party", "betrayal", "chat"),
                        List.of("social deduction", "asymmetric information", "group voting"),
                        List.of("TTS", "STT"), 88, 93, 94, 84, 610_000, 0.92),
                new LegacyGameSeed("Keep Talking and Nobody Explodes", "STEAM", "341800", 341800, 2015,
                        List.of("Puzzle", "Co-op"), List.of("co-op", "communication", "voice", "party"),
                        List.of("asymmetric co-op", "verbal puzzle", "time pressure"),
                        List.of("STT", "TTS"), 95, 90, 97, 82, 13_000, 0.98),
                new LegacyGameSeed("Papers, Please", "STEAM", "239030", 239030, 2013,
                        List.of("Simulation", "Narrative"), List.of("bureaucracy", "moral choice", "document"),
                        List.of("document inspection", "moral pressure", "branching narrative"),
                        List.of("TTS"), 91, 72, 68, 88, 72_000, 0.97),
                new LegacyGameSeed("Vampire Survivors", "STEAM", "1794680", 1794680, 2022,
                        List.of("Roguelike", "Survival"), List.of("survival", "roguelike", "auto battler"),
                        List.of("minimal control", "horde survival", "build crafting"),
                        List.of("TTS"), 86, 84, 76, 91, 240_000, 0.98),
                new LegacyGameSeed("Garry's Mod", "STEAM", "4000", 4000, 2006,
                        List.of("Sandbox", "Multiplayer"), List.of("sandbox", "modding", "roleplay", "party"),
                        List.of("sandbox creation", "user generated modes", "social roleplay"),
                        List.of("WEBCAM", "TTS", "STT"), 94, 96, 95, 65, 950_000, 0.96),
                new LegacyGameSeed("Slay the Spire", "STEAM", "646570", 646570, 2019,
                        List.of("Deckbuilder", "Roguelike"), List.of("deckbuilding", "roguelike", "strategy"),
                        List.of("deck drafting", "run-based progression", "risk choice"),
                        List.of("TTS"), 89, 74, 66, 80, 160_000, 0.97),
                new LegacyGameSeed("The Stanley Parable", "STEAM", "221910", 221910, 2013,
                        List.of("Narrative", "Comedy"), List.of("narrative", "choice", "meta", "comedy"),
                        List.of("narrator interaction", "branching paths", "meta humor"),
                        List.of("TTS", "STT"), 93, 80, 86, 76, 48_000, 0.93),
                new LegacyGameSeed("Don't Starve Together", "STEAM", "322330", 322330, 2016,
                        List.of("Survival", "Co-op"), List.of("survival", "co-op", "crafting"),
                        List.of("shared survival", "resource pressure", "emergent failure"),
                        List.of("TTS", "STT"), 84, 88, 82, 78, 340_000, 0.95),
                new LegacyGameSeed("Project Zomboid", "STEAM", "108600", 108600, 2013,
                        List.of("Survival", "Sandbox"), List.of("zombie", "survival", "sandbox", "roleplay"),
                        List.of("deep survival simulation", "emergent story", "sandbox roleplay"),
                        List.of("WEBCAM", "TTS"), 90, 86, 84, 70, 260_000, 0.94)
        );
    }
}
