# 04_Mvp_Scope.md

## Minimum Viable Product (MVP) Scope
The MVP will focus on establishing the core data pipeline and a basic recommendation engine based solely on the provided seed data, without integrating real-time webcam/TTS features yet.

## In Scope (MVP)
1. **Data Loading:** Successfully load and parse `games_seed.json`.
2. **Basic Analysis:** Implement the logic to calculate simple genre popularity trends from the seed data.
3. **Seed Recommendation:** Implement a basic recommendation function based on predefined success metrics.
4. **Prompt Implementation:** Establish working prompts for the AI agent pipeline.

## Out of Scope (Future Phases)
1. **Real-time Integration:** Integrating webcam, TTS, or STT functionality into the backend flow.
2. **External Data Integration:** Connecting to external, live game databases.
3. **Advanced ML Models:** Implementing complex machine learning for deeper trend prediction.