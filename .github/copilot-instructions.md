# Multi-Stage TDD Prompt for GitHub Copilot: Extending personai-gallery with AI Persona & Memory System
`.github/copilot-instruction.md`<br>
**Context:** You are working on a fork of the Google AI Edge Gallery. The primary goal is to develop a personalized AI gallery. The absolute rule is: **do not modify or break any existing files**. You will exclusively **add new files** and extend functionalities by creating new modules that integrate seamlessly.<br> But regarding the frontend u are free to choose architectural descision (it should strictly follow th test driven integration tests) but update this file based  on that.

## Stage 0: Infrastructure & Analysis (No Code Changes)
1.  **Read the Repo Structure:** Before any code generation, analyze the existing Android project structure (`/Android/src/`). Familiarize yourself with the existing modules (e.g., `com.google.ai.edge.gallery` package) and `AndroidManifest.xml`.
2.  **Plan New Packages:** All new code will be placed in new packages like `com.personai.agent`, `com.personai.memory`, `com.personai.persona`, `com.personai.notification`, `com.personai.overlay`, `com.personai.sync`. This ensures zero interference with the original `com.google.ai.edge.gallery` code.

## Stage 1: Contextual Persona Engine (Notification Scraping)
**Feature:** Build a dynamic persona by silently scraping user notifications.
**New Files:**
- `PersonaNotificationService.kt` (extends `NotificationListenerService`)
- `PersonaInferenceEngine.kt` (uses ML Kit for analysis)
- `PersonaDatabase.kt` (Room database for persona snapshots)

**Implementation Steps (TDD):**
1.  **Write Test:** `given valid notification bundle when posted then extracted data saved correctly()`.
2.  **Implement:** The `PersonaNotificationService` will listen for notifications, extract text, app package, and timestamp. Store in `notification_events` table.
3.  **Write Test:** `given work notifications during 9-5 when analyzed then persona state set to FOCUSED_WORK()`.
4.  **Implement:** `PersonaInferenceEngine` will, as a lightweight `WorkManager` task, process batches of notifications to infer user intent, emotional state, and topics. This will use ML Kit's `EntityExtraction` and `TextClassification` for on-device analysis.
5.  **Key Libraries:** `androidx.work:work-runtime-ktx`, `com.google.mlkit:entity-extraction`, `com.google.mlkit:text-recognition`. Append the critical elements of the plan in this file.

## Stage 2: Memory System with File-Prompt Linking
**Feature:** Every file the user sees and every prompt they make is embedded and stored for retrieval.
**New Files:**
- `MemoryVectorizer.kt` (Generates embeddings for images/text)
- `MemoryRepository.kt` (Handles storage and similarity search)
- `FilePromptLinker.kt` (Manages relationships between files and user prompts)

**Implementation Steps (TDD):**
1.  **Write Test:** `given image file when embedded then vector generated with correct dimensions()`.
2.  **Implement:** `MemoryVectorizer` will use `LiteRT` and a quantized `MobileNetV3` model to generate image embeddings. For text, it will leverage a `Gemma 4` or `Universal Sentence Encoder Lite`.
3.  **Write Test:** `given query embedding when searching then top K similar memories retrieved()`.
4.  **Implement:** `MemoryRepository` will store embeddings in a new `memory_items` table in a SQLite database using the `sqlite-vector` extension. Implement a `similarity_search()` function that uses cosine distance.
5.  **Key Libraries:** `org.tensorflow:tensorflow-lite`, `androidx.room:room-ktx`, `com.github.sqlite-vector:sqlite-vector`.

## Stage 3: Background User Activity Analyzer
**Feature:** Lightweight background task to learn user activity patterns.
**New Files:**
- `ActivityAnalyzerService.kt` (Uses `UsageStatsManager`)
- `UsagePatternModel.kt` (Handles anomaly detection)

**Implementation Steps (TDD):**
1.  **Write Test:** `when device in doze mode then analysis deferred until idle period()`.
2.  **Implement:** `ActivityAnalyzerService` will be a `WorkManager` periodic task (every 30 minutes) that queries `UsageStatsManager` for app usage data. It will be constrained to run only when the device is idle and battery is not low.
3.  **Write Test:** `when background analyzer running then battery drain less than 2% per day()`.
4.  **Implement:** The `UsagePatternModel` will perform frequency analysis on app usage patterns to detect routines and anomalies, using a custom TensorFlow Lite model for efficiency.
5.  **Key Libraries:** `androidx.work:work-runtime`, `org.tensorflow:tensorflow-lite`.

## Stage 4: On-Device AI Agent
**Feature:** An AI agent capable of dynamic app interaction and Google Search.
**New Files:**
- `AgentCore.kt` (Integrates with `FunctionGemma 270M`)
- `UIAutomationHelper.kt` (Uses `AccessibilityService`)
- `OverlayChatService.kt` (Floating chat window)

**Implementation Steps (TDD):**
1.  **Write Test:** `given natural language command when parsed then correct function selected()`.
2.  **Implement:** `AgentCore` will load the `FunctionGemma 270M` model and use `androidx.appfunctions:appfunctions` to expose gallery operations as callable functions for the LLM.
3.  **Write Test:** `given target app element when located then click action performed successfully()`.
4.  **Implement:** `UIAutomationHelper` will use an `AccessibilityService` to find UI elements by text or ID and perform clicks. For Google Search, it will use the `Custom Search JSON API` (API key required).
5.  **Write Test:** `when overlay launched then window appears above all activities()`.
6.  **Implement:** `OverlayChatService` will use `WindowManager` to create a chat head overlay, allowing users to interact with the agent from anywhere.
7.  **Key Libraries:** `com.google.ai.edge:functiongemma`, `androidx.appfunctions:appfunctions`, `com.google.android.gms:play-services-auth`.

## Stage 5: Obsidian Integration
**Feature:** Two-way sync between the app's memory and an Obsidian vault.
**New Files:**
- `ObsidianSyncManager.kt` (Handles file-based sync)
- `MarkdownParser.kt` (Parses `.md` files with frontmatter)

**Implementation Steps (TDD):**
1.  **Write Test:** `given new memory when exported then markdown file created in Obsidian vault()`.
2.  **Implement:** `ObsidianSyncManager` will monitor a user-specified folder (the Obsidian vault) using `FileObserver`. It will export new `MemoryItem` objects as Markdown files with YAML frontmatter containing metadata.
3.  **Write Test:** `when Obsidian file changes then app memory updated with new content()`.
4.  **Implement:** `MarkdownParser` will read Markdown files, parse the frontmatter and content, and update the local database accordingly.
5.  **Key Libraries:** `org.yaml:snakeyaml`, `androidx.documentfile:documentfile`.

## Global Requirements
- **Test-Driven Development:** For every new feature, write a test first. Use `JUnit5`, `MockK`, and `Robolectric`.
- **Context7 MCP:** Before writing code for any new library (e.g., `FunctionGemma`, `sqlite-vector`), use the `context7` tool to fetch the latest documentation and ensure the code is correct.
- **Privacy & Performance:** All inference must be on-device. All background tasks must be constrained (`WorkManager` with idle/battery conditions). Target <2% battery drain per day.

## Executing the Prompt
To implement each stage, deploy agent: "Based on the existing `personai-gallery` structure (which remains unmodified), implement `Stage X` (e.g., 'Contextual Persona Engine') following the TDD steps detailed in the prompt."
