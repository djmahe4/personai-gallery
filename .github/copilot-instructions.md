# Multi-Stage TDD Prompt for GitHub Copilot: Extending personai-gallery with AI Persona, Memory System, Social Matching, Quantum-Inspired Reasoning, Deep Wiki, and PDF Reasoning

`.github/copilot-instructions.md`  
**Context:** You are working on a fork of the Google AI Edge Gallery. The primary goal is to develop a personalized AI gallery. The absolute rule is: **do not modify or break any existing files**. You will exclusively **add new files** and extend functionalities by creating new modules that integrate seamlessly. For the frontend you are free to choose architectural decisions, provided every new frontend component is covered by **test‑driven integration tests** that validate the interaction with the new modules.

---

## Behavioral Science & Marketing Psychology Architecture (PLFS Evaluated)

Applied mental models guide user engagement, cognitive retention, task triage, and system performance without friction or manipulation. Every model is evaluated using the **Psychological Leverage & Feasibility Score (PLFS)**:
`PLFS = (Leverage + Fit + Speed + Ethics) - Implementation Cost` (Range: -5 to +15).

---

### Mental Model 1: Ebbinghaus Forgetting Curve & Spaced Retrieval Practice
**PLFS:** `+15` (Leverage: 5, Fit: 5, Speed: 4, Ethics: 5, Cost: 2) — *High-confidence lever*

* **Why it works (psychology)**:
  Human memory retention decays exponentially over time: $R(t) = e^{-t/S}$. Without systematic reinforcement, ~70% of learned material is forgotten within 48 hours. Providing frictionless, spaced retrieval practice at mathematically optimal intervals resets the decay curve and flattens the forgetting slope, cementing long-term memory with minimal cognitive overhead.
* **Behavior targeted**:
  User engages with spaced concept revision, long-term learning tasks, and deep recall without feeling spammed.
* **Where to apply**:
  `com.personai.memory` and `com.personai.learning`: Memory retention ranking, concept auto-linking in Deep Wiki, proactive bite-sized revision prompts in Overlay Chat.
* **How to implement**:
  1. Store stability factor $S$ and last-reviewed timestamp for each memory and concept node.
  2. Implement continuous retention calculation $R(t) = e^{-t/S}$.
  3. When $R(t) < 0.45$, schedule an unobtrusive revision card into the user's priority feed.
  4. Upon successful recall/review, multiply stability $S$ using SM-2 spacing factors ($S_{n+1} = S_n \times \text{Factor}$).
* **What to test**:
  - Decay curve accuracy: retention drops as expected over simulated time intervals.
  - Spacing multiplication: stability increases with each successful revision.
  - Notification threshold: revisions trigger only when memory decay passes the retention threshold.
* **Ethical guardrail**:
  No coercive streaks or guilt-inducing notifications. The user controls revision topics and can snooze or silence review intervals at will.

---

### Mental Model 2: Eisenhower Decision Matrix & Paradox of Choice (Hick's Law)
**PLFS:** `+15` (Leverage: 5, Fit: 5, Speed: 5, Ethics: 5, Cost: 2) — *High-confidence lever*

* **Why it works (psychology)**:
  Exposing users to unsorted, flat task streams creates decision fatigue, paralysis, and default avoidance. Filtering tasks into 4 distinct quadrants (Urgent vs. Important) provides instant cognitive clarity. Emphasizing Quadrant 2 (Not Urgent, but Important) shifts behavior from reactive anxiety to proactive growth and mastery.
* **Behavior targeted**:
  User takes decisive action on top-priority tasks, schedules deep learning, and delegates routine toil to the on-device agent.
* **Where to apply**:
  `com.personai.task` and `com.personai.agent`: Task classification, daily suggestion feed, agent action planner.
* **How to implement**:
  1. Classify incoming actions/notifications into 4 quadrants:
     - **Q1 (Urgent & Important - Do Now)**: Immediate blockers, critical alerts.
     - **Q2 (Not Urgent & Important - Schedule & Protect)**: Spaced concept revision, deep reading, personal goals.
     - **Q3 (Urgent & Not Important - Delegate)**: Routine confirmations, file organization, agent-executable queries.
     - **Q4 (Not Urgent & Not Important - Eliminate/Silent)**: Ephemeral clutter, low-signal pings.
  2. Present maximum 3 actionable recommendations at a time (Hick's Law).
  3. Provide 1-click execution for Q3 agent tasks.
* **What to test**:
  - Classifier correctly maps deadline + significance signals to quadrants.
  - UI never presents more than 3 high-priority recommendations simultaneously.
  - Q2 tasks are surfaced during designated focus windows.
* **Ethical guardrail**:
  Transparent classification rationale. Users can reclassify any task and override agent prioritization.

---

### Mental Model 3: Friction Minimization & Loss Aversion (Resource-Aware Background Queuing)
**PLFS:** `+14` (Leverage: 4, Fit: 5, Speed: 4, Ethics: 5, Cost: 2) — *High-confidence lever*

* **Why it works (psychology)**:
  Perceived lag, stuttering UI, battery drain, and thermal throttling trigger severe loss aversion (fear of device degradation, battery depletion, and app unresponsiveness). By throttling CPU usage, pacing background jobs, and queuing tasks gracefully during idle/charging windows, the app delivers a whisper-quiet, frictionless experience that builds trust and retention.
* **Behavior targeted**:
  User keeps PersonAI running continuously in the background without worrying about battery drain or UI lag.
* **Where to apply**:
  `com.personai.queue` and `com.personai.sync`: Embedding generation, OCR text extraction, ML inference, and database syncing.
* **How to implement**:
  1. Enforce a single-threaded paced queue for heavy ML inference tasks (LiteRT/Gemma/Embedding).
  2. Implement `AdaptiveCpuThrottler` monitoring device battery level and thermal state (`PowerManager` and `ThermalStatusListener`).
  3. Defer non-critical vectorization and wiki indexing to `WorkManager` jobs with `setRequiresDeviceIdle(true)` and `setRequiresBatteryNotLow(true)`.
  4. Cap peak background CPU consumption below 15% during interactive foreground sessions.
* **What to test**:
  - Queue limits concurrency and throttles jobs when thermal status reaches moderate/severe.
  - Heavy background batch jobs automatically yield during user interaction.
  - Overall battery drain remains < 2% per 24 hours.
* **Ethical guardrail**:
  Never conceal background resource consumption. Provide full transparency on battery and storage utilization in settings.

---

### Mental Model 4: Endowment Effect & Zeigarnik Effect (Knowledge Node Completion)
**PLFS:** `+14` (Leverage: 4, Fit: 5, Speed: 4, Ethics: 5, Cost: 2) — *High-confidence lever*

* **Why it works (psychology)**:
  People value systems they have contributed to building (Endowment Effect / IKEA Effect), and incomplete tasks remain active in working memory until resolved (Zeigarnik Effect). Showing users their interconnected knowledge graph and open concept loops motivates continuous, natural learning without synthetic gamification.
* **Behavior targeted**:
  User regularly links personal memories to wiki nodes and completes open concept revisions.
* **Where to apply**:
  `com.personai.wiki` and `com.personai.learning`: Graph visualization, concept mastery indicators, and PDF chunk review loops.
* **How to implement**:
  1. Show visual connection strength between user-uploaded files and persona concepts.
  2. Display subtle visual indicators for concept nodes nearing retention decay thresholds.
  3. Close open loops with gentle summaries once revisions are completed.
* **What to test**:
  - Graph links reflect user-curated associations with higher weights than purely automated links.
  - Revision completion updates retention status and clears pending review queues.
* **Ethical guardrail**:
  No artificial anxiety or punitive deadlines. Knowledge graph is completely local and user-owned.

---

## Stage 0: Infrastructure & Analysis (No Code Changes)
1. **Read the Repo Structure:** Before any code generation, analyze the existing Android project structure (`/Android/src/`). Familiarize yourself with the existing modules (e.g., `com.google.ai.edge.gallery` package) and `AndroidManifest.xml`.
2. **Plan New Packages:** All new code will be placed in new packages like `com.personai.agent`, `com.personai.memory`, `com.personai.learning`, `com.personai.task`, `com.personai.queue`, `com.personai.persona`, `com.personai.notification`, `com.personai.overlay`, `com.personai.sync`, `com.personai.match`, `com.personai.quantum`, `com.personai.wiki`, `com.personai.pdfreasoner`. This ensures zero interference with the original `com.google.ai.edge.gallery` code.

## Stage 1: Contextual Personas Engine (Notification Scraping)
**Feature:** Build dynamic personas (for both user and others) by silently scraping user notifications and caching and processing at intervals. (Allow user the option to choose the people to track and also cross relate across sm platforms).
**New Files:**
- `PersonaNotificationService.kt` (extends `NotificationListenerService`)
- `PersonaInferenceEngine.kt` (uses ML Kit for analysis)
- `PersonaDatabase.kt` (Room database for persona snapshots)

**Implementation Steps (TDD):**
1. **Write Test:** `given valid notification bundle when posted then extracted data saved correctly()`.
2. **Implement:** The `PersonaNotificationService` will listen for notifications, extract text, app package, and timestamp. Store in `notification_events` table.
3. **Write Test:** `given work notifications during 9-5 when analyzed then persona state set to FOCUSED_WORK()`.
4. **Implement:** `PersonaInferenceEngine` will, as a lightweight `WorkManager` task, process batches of notifications to infer user intent, emotional state, and topics. This will use ML Kit's `EntityExtraction` and `TextClassification` for on-device analysis.
5. **Key Libraries:** `androidx.work:work-runtime-ktx`, `com.google.mlkit:entity-extraction`, `com.google.mlkit:text-recognition`.

## Stage 2: Memory System with File-Prompt Linking
**Feature:** Every file the user sees and every prompt they make is embedded and stored for retrieval.
**New Files:**
- `MemoryVectorizer.kt` (Generates embeddings for images/text)
- `MemoryRepository.kt` (Handles storage and similarity search)
- `FilePromptLinker.kt` (Manages relationships between files and user prompts)

**Implementation Steps (TDD):**
1. **Write Test:** `given image file when embedded then vector generated with correct dimensions()`.
2. **Implement:** `MemoryVectorizer` will use `LiteRT` and a quantized `MobileNetV3` model to generate image embeddings. For text, it will leverage a `Gemma 4` or `Universal Sentence Encoder Lite`.
3. **Write Test:** `given query embedding when searching then top K similar memories retrieved()`.
4. **Implement:** `MemoryRepository` will store embeddings in a new `memory_items` table in a SQLite database using the `sqlite-vector` extension. Implement a `similarity_search()` function that uses cosine distance.
5. **Key Libraries:** `org.tensorflow:tensorflow-lite`, `androidx.room:room-ktx`, `com.github.sqlite-vector:sqlite-vector`.

## Stage 2.1: Ebbinghaus Memory Decay & Spaced Concept Revision Engine
**Feature:** Integrate an Ebbinghaus forgetting curve memory decay mechanism with concept revision scheduling. As time passes without interaction, memory retention scores decay exponentially ($R = e^{-t/S}$). Concepts below retention thresholds are surfaced for spaced revision (e.g., after 1 day, 3 days, 7 days, 14 days, 30 days, 60 days) to solidify long-term retention.
**New Files:**
- `EbbinghausDecayEngine.kt` (Calculates retention scores based on elapsed time and memory stability $S$)
- `SpacedRepetitionScheduler.kt` (Calculates optimal next-revision dates using SM-2 spaced repetition multipliers)
- `ConceptRevisionWorker.kt` (WorkManager job executing during device idle to update decay scores and flag concepts due for review)
- `MemoryRetentionDao.kt` (Room database queries for retrieving decaying concepts and logging review attempts)

**Implementation Steps (TDD):**
1. **Write Test:** `given concept memory with stability S when 7 days elapse without review then retention score reflects exponential decay()`.
2. **Implement:** `EbbinghausDecayEngine.calculateRetention(stability, elapsedDays)` implementing $R = e^{-t / S}$.
3. **Write Test:** `given concept reviewed successfully with good recall when next interval scheduled then stability S increases()`.
4. **Implement:** `SpacedRepetitionScheduler.computeNextInterval(currentStability, recallQuality)` returning the new stability and target revision timestamp.
5. **Write Test:** `given concepts with retention below 45 percent when revision worker executes then concept revision tasks queued()`.
6. **Implement:** `ConceptRevisionWorker` queries `MemoryRetentionDao` for items where $R(t) < 0.45$, generates proactive concept revision cards, and sends them to the task queue.
7. **Key Libraries:** `androidx.work:work-runtime-ktx`, `androidx.room:room-ktx`, `kotlin.math`.

## Stage 3: Background User Activity Analyzer
**Feature:** Lightweight background task to learn user activity patterns without battery strain.
**New Files:**
- `ActivityAnalyzerService.kt` (Uses `UsageStatsManager`)
- `UsagePatternModel.kt` (Handles anomaly detection)

**Implementation Steps (TDD):**
1. **Write Test:** `when device in doze mode then analysis deferred until idle period()`.
2. **Implement:** `ActivityAnalyzerService` will be a `WorkManager` periodic task (every 30 minutes) that queries `UsageStatsManager` for app usage data. It will be constrained to run only when the device is idle and battery is not low.
3. **Write Test:** `when background analyzer running then battery drain less than 2% per day()`.
4. **Implement:** The `UsagePatternModel` will perform frequency analysis on app usage patterns to detect routines and anomalies, using a custom TensorFlow Lite model for efficiency.
5. **Key Libraries:** `androidx.work:work-runtime`, `org.tensorflow:tensorflow-lite`.

## Stage 3.1: Eisenhower Matrix Task Prioritization & Smart Suggestion Engine
**Feature:** Dynamic classification of all user notifications, learning goals, concept revisions, and agent activities into the Eisenhower 2x2 Matrix (Urgent vs. Important). Prevents cognitive overload by prioritizing Quadrant 2 (Important, Not Urgent) growth tasks while batching or delegating Quadrant 3 tasks to the on-device agent.
**New Files:**
- `EisenhowerTaskClassifier.kt` (Classifies tasks and notifications into Q1, Q2, Q3, Q4)
- `TaskPriorityAdvisor.kt` (Recommends next actions, respecting Hick's Law by capping to max 3 focal suggestions)
- `QuadrantQueueManager.kt` (Manages priority execution queues based on Eisenhower quadrants)
- `TaskSuggestionCard.kt` (UI data model for presenting prioritized suggestions in overlay and gallery)

**Implementation Steps (TDD):**
1. **Write Test:** `given urgent system alert when classified then placed in Quadrant 1 Do Immediately()`.
2. **Implement:** `EisenhowerTaskClassifier.classify(task)` assessing urgency flags, deadlines, and importance weights.
3. **Write Test:** `given spaced concept revision task when classified then placed in Quadrant 2 Schedule and Protect Focus()`.
4. **Implement:** Connect `ConceptRevisionWorker` outputs to Quadrant 2 of `QuadrantQueueManager`, scheduling them during user-preferred learning windows.
5. **Write Test:** `given repetitive routine query when classified then placed in Quadrant 3 Delegate to Agent()`.
6. **Implement:** `QuadrantQueueManager` auto-routes Q3 tasks to `AgentCore` for 1-click or automated local execution.
7. **Write Test:** `when advisor generates suggestions then at most 3 prioritized items returned()`.
8. **Implement:** `TaskPriorityAdvisor.getTopSuggestions()` filters and ranks items to minimize decision fatigue.
9. **Key Libraries:** `androidx.room:room-ktx`, `kotlinx.coroutines`.

## Stage 3.2: Paced Background Task Queue & Adaptive CPU Throttler
**Feature:** Intelligent resource governor that queues and paces background ML processing (vector embeddings, OCR, entity extraction) rather than consuming excessive CPU. Defers or throttles tasks based on device thermal status, battery level, and user interactivity to preserve buttery-smooth UI performance.
**New Files:**
- `AdaptiveWorkQueue.kt` (Prioritized, rate-paced queue managing background ML jobs)
- `CpuPacingGovernor.kt` (Monitors thermal state and CPU load, injecting pauses between heavy batches)
- `ThermalBatteryMonitor.kt` (Listens to `PowerManager.OnThermalStatusChangedListener` and battery level broadcasts)

**Implementation Steps (TDD):**
1. **Write Test:** `given 50 pending embedding jobs when queued then jobs processed with inter-job pacing without UI freeze()`.
2. **Implement:** `AdaptiveWorkQueue` uses Kotlin Coroutines with a bounded channel (`concurrency = 1`) and dynamic yield delays.
3. **Write Test:** `given device thermal status is THERMAL_STATUS_MODERATE when heavy job dispatched then job execution throttled()`.
4. **Implement:** `CpuPacingGovernor` queries `ThermalBatteryMonitor` and adds exponential pacing delay or pauses the queue until temperature normalizes.
5. **Write Test:** `when battery falls below 20 percent and not charging then non-critical background jobs paused()`.
6. **Implement:** `AdaptiveWorkQueue.pauseNonCriticalJobs()` until charging state or battery recovery broadcast is received.
7. **Key Libraries:** `android.os.PowerManager`, `kotlinx.coroutines.channels`.

## Stage 4: On-Device AI Agent
**Feature:** An AI agent capable of dynamic app interaction and Google Search.
**New Files:**
- `AgentCore.kt` (Integrates with `FunctionGemma 270M`)
- `UIAutomationHelper.kt` (Uses `AccessibilityService`)
- `OverlayChatService.kt` (Floating chat window)

**Implementation Steps (TDD):**
1. **Write Test:** `given natural language command when parsed then correct function selected()`.
2. **Implement:** `AgentCore` will load the `FunctionGemma 270M` model and use `androidx.appfunctions:appfunctions` to expose gallery operations as callable functions for the LLM.
3. **Write Test:** `given target app element when located then click action performed successfully()`.
4. **Implement:** `UIAutomationHelper` will use an `AccessibilityService` to find UI elements by text or ID and perform clicks. For Google Search, it will use the `Custom Search JSON API` (API key required).
5. **Write Test:** `when overlay launched then window appears above all activities()`.
6. **Implement:** `OverlayChatService` will use `WindowManager` to create a chat head overlay, allowing users to interact with the agent from anywhere.
7. **Key Libraries:** `com.google.ai.edge:functiongemma`, `androidx.appfunctions:appfunctions`, `com.google.android.gms:play-services-auth`.

## Stage 5: Obsidian Integration
**Feature:** Two-way sync between the app's memory and an Obsidian vault.
**New Files:**
- `ObsidianSyncManager.kt` (Handles file-based sync)
- `MarkdownParser.kt` (Parses `.md` files with frontmatter)

**Implementation Steps (TDD):**
1. **Write Test:** `given new memory when exported then markdown file created in Obsidian vault()`.
2. **Implement:** `ObsidianSyncManager` will monitor a user-specified folder (the Obsidian vault) using `FileObserver`. It will export new `MemoryItem` objects as Markdown files with YAML frontmatter containing metadata.
3. **Write Test:** `when Obsidian file changes then app memory updated with new content()`.
4. **Implement:** `MarkdownParser` will read Markdown files, parse the frontmatter and content, and update the local database accordingly.
5. **Key Libraries:** `org.yaml:snakeyaml`, `androidx.documentfile:documentfile`.

---

## Stage 6: Write-Time Matching System (O(1) Persona Connections)
**Feature:** Instant, privacy‑first matching when two users express mutual interest. The system is designed for O(1) per action: when User A likes User B, it instantly checks if B already liked A; if yes a match is created, otherwise the directed like is stored. No heavy processing on reveal day, and all matching logic runs on‑device with anonymised identifiers.

**New Files:**
- `MatchGraphRepository.kt` (Room entity for directed likes, match‑pairs)
- `MatchService.kt` (Business logic for O(1) write‑time matching)
- `PrivacyHasher.kt` (One‑way hashing of user identifiers to preserve anonymity)

**Implementation Steps (TDD):**
1. **Write Test:** `given user A likes user B and B has not liked A when like stored then match table remains empty`.
2. **Implement:** `MatchService.recordLike(fromHash, toHash)` will insert a `Like` row if not exists. After insertion it queries for a reciprocal like: `SELECT 1 FROM likes WHERE fromHash = toHash AND toHash = fromHash`. If found, a `Match` row is created. All queries are indexed and run in O(1) effective time.
3. **Write Test:** `given both users have liked each other when reciprocal like processed then match created and both parties notified`.
4. **Implement:** The `MatchService` uses `Room` with appropriate indices `(fromHash, toHash)` and a unique constraint. Notifications are sent via `PersonaNotificationService` to the matched user’s gallery.
5. **Write Test:** `given anonymous identifiers when stored then no plain‑text email exposed`.
6. **Implement:** `PrivacyHasher` uses SHA‑256 with a per‑installation salt to convert user‑identifying attributes (e.g., college email) into irreversible hashes, so the local database never contains raw PII.
7. **Key Libraries:** `androidx.room:room-ktx`, `com.google.crypto.tink:tink-android`.

## Stage 7: Quantum-Inspired Deep Reasoning Engine
**Feature:** A reasoning layer that treats persona snapshots, memory vectors, and activity states as quantum‑like states – superposition, state flipping, and graph rotations – to obtain multi‑dimensional summaries and drive deeper context understanding. This engine enriches the agent’s decisions and the wiki’s automatic linking.

**New Files:**
- `QubitPersonaState.kt` (Encodes persona as a complex probability amplitude vector)
- `QuantumReasoningEngine.kt` (Applies unitary transformations, measurement, and graph rotations)
- `SuperpositionGraph.kt` (Manages relationships between multiple simultaneous states)
- `DimensionalSummarizer.kt` (Projects high‑dimensional qubit states into human‑interpretable summaries)

**Implementation Steps (TDD):**
1. **Write Test:** `given work and leisure persona dimensions when superposition created then state vector has equal amplitudes for both`.
2. **Implement:** `QubitPersonaState` uses a small fixed number of basis states (e.g., 8) corresponding to dominant persona modes. Amplitudes are stored as two `FloatArray` (real and imaginary parts). Operations like `superpose(stateA, stateB, alpha)` compute new amplitudes.
3. **Write Test:** `given a state with work amplitude 0.8 when a flip operation applied then work amplitude becomes 0.8 * -1 (phase flip)`.
4. **Implement:** `QuantumReasoningEngine.applyFlip(basisIndex)` multiplies the amplitude of that basis by -1. Graph rotations are implemented as a unitary matrix multiplication (e.g., rotation on a pair of basis states). All operations are performed on‑device with Kotlin’s `kotlin.math` or a tiny `Eigen`‑like library.
5. **Write Test:** `given memory embedding and quantum state when graph rotation performed then nearest links realigned`.
6. **Implement:** `SuperpositionGraph` represents each wiki node as a qubit; rotations adjust the correlation strength between nodes, enabling emergent deep reasoning connections. `DimensionalSummarizer` performs a projective measurement (collapses the state) to produce a deterministic summary for the UI.
7. **Key Libraries:** `org.apache.commons:commons-math3` (for complex number support), possibly `com.github.nicklaus4:complexkt` – prefer Kotlin native complex implementations to minimise dependencies. All logic kept under `com.personai.quantum`.

## Stage 8: Deep Wiki with Bidirectional Links (The Memory Core)
**Feature:** A fully local, graph‑based wiki that automatically links memories, files, prompts, persona snapshots, concept decay states, and PDF‑indexed chunks. Users can view the graph, create new links, and update relationships. The wiki becomes the central browsing interface for the user’s knowledge, replacing the simple file list with a visual network. Allow the user to link photos or videos as memories with lightweight index, re-ID mechanisms, and body language assessment capabilities.

**New Files:**
- `WikiGraphDatabase.kt` (Room entities for nodes and edges, plus graph queries)
- `WikiAutoLinker.kt` (Heuristic and quantum‑engine‑driven automatic link creation)
- `WikiFrontendFragment.kt` (Interactive web view or Canvas‑based frontend for exploring the graph)
- `WikiEditorActivity.kt` (Allows manual addition/deletion of links)

**Implementation Steps (TDD):**
1. **Write Test:** `given a new memory node when auto‑linker runs then edges created to top‑k similar nodes`.
2. **Implement:** `WikiGraphDatabase` uses three tables: `wiki_nodes`, `wiki_edges`, and `wiki_link_log`. Each node has a type (MEMORY, FILE, PROMPT, PERSONA_SNAPSHOT, PDF_CHUNK, CONCEPT) and a JSON payload. Edges store a weight, `decay_factor`, and `link_type` (AUTO, MANUAL). `WikiAutoLinker` calls `MemoryRepository.similarity_search()` and also uses the quantum engine’s superposition graph to propose non‑obvious links.
3. **Write Test:** `when user manually links two nodes then edge appears and auto‑link weight adjusted`.
4. **Implement:** `WikiEditorActivity` exposes a searchable node picker. Manual links are stored with `link_type = MANUAL`. The `WikiFrontendFragment` renders nodes and edges using a `WebView` with a JavaScript graph library (e.g., `vis‑network`) served from local assets, and communicates via a `@JavascriptInterface`. Integration tests will verify that tapping a node triggers the correct gallery action.
5. **Write Test:** `given PDF chunk node when tapped then PDF viewer opens at the chunk’s page`.
6. **Implement:** Nodes carry a `referenceURI` (e.g., a content URI to the PDF with page fragment). The frontend dispatches an intent to open the PDF at the indexed location.
7. **Key Libraries:** `androidx.webkit:webkit`, `com.google.code.gson:gson` (for node metadata). Frontend tests use `Espresso` and `WebView` interactions.

## Stage 9: User‑Uploaded PDF Indexing with Page‑Range Selection (Gemma‑Powered Reasoning)
**Feature:** The user uploads a PDF file, selects a page range, and the app renders each page to a bitmap, extracts text via on‑device OCR, passes the text to the local Gemma model for reasoning and summarisation, then indexes the chunks as wiki nodes with back‑links to the original PDF pages. This enables semantic search inside PDFs without any data leaving the device.

**New Files:**
- `PDFPickerHandler.kt` (Launches a system file picker, validates that the selected file is a PDF)
- `PageRangeSelectorDialog.kt` (UI component letting the user pick a start/end page)
- `PDFPageRenderer.kt` (Uses `PdfRenderer` to render a specific page to a Bitmap)
- `OCRTextExtractor.kt` (On‑device ML Kit text recognition from the rendered Bitmap)
- `GemmaReasoningClient.kt` (Calls the locally loaded Gemma model for chunk summarisation)
- `PDFIndexer.kt` (Orchestrates the flow: render → OCR → Gemma → store as wiki nodes)

**Implementation Steps (TDD):**
1. **Write Test:** `given a valid PDF URI when parsed with PdfRenderer then page count is correctly determined`.
2. **Implement:** `PDFPickerHandler` uses `ActivityResultContracts.OpenDocument` to let the user pick a file; it filters for `application/pdf`. On success it opens an `InputStream` to pass to `PdfRenderer`. `PageRangeSelectorDialog` shows the page count and lets the user choose a range.
3. **Write Test:** `given a page range 2-4 when renderer called then bitmap for page 3 is returned with correct dimensions`.
4. **Implement:** `PDFPageRenderer` uses Android’s `PdfRenderer` (`android.graphics.pdf.PdfRenderer`) to render each page in the selected range to a `Bitmap`. The renderer is light‑weight and runs entirely on‑device.
5. **Write Test:** `given a rendered page bitmap when OCR runs then text extracted with accuracy > 90%`.
6. **Implement:** `OCRTextExtractor` uses `com.google.mlkit:text-recognition` with the latest Latin text recogniser. It returns the raw string.
7. **Write Test:** `given extracted page text when Gemma reasoning requested then summary JSON returned with topics, entities, and relationships`.
8. **Implement:** `GemmaReasoningClient` loads the same `FunctionGemma 270M` model as the agent, prompted to output a structured JSON with keys like `summary`, `topics`, `entities`, and `relationships`. All computation stays on‑device.
9. **Write Test:** `when PDF indexing completes then a wiki node of type PDF_CHUNK created per page with correct page metadata and auto‑links to adjacent pages`.
10. **Implement:** `PDFIndexer` iterates over the selected page range, calls render/OCR/Gemma for each page, then inserts a `wiki_node` of type `PDF_CHUNK` into the `WikiGraphDatabase`. The node stores the content URI of the original PDF, the page number, the OCR text, and the Gemma‑generated summary. Auto‑links are created between consecutive pages and to any relevant existing memory nodes (via similarity search).
11. **Write Test:** `when PDF chunk node tapped in wiki then PDF viewer opens at the exact page`.
12. **Implement:** The node’s `referenceURI` is a content URI with a fragment `?page=3`. The wiki frontend opens an `Intent` with that URI; a dedicated PDF viewer activity (or an external viewer) interprets the fragment to scroll to the specified page. No data leaves the device.
13. **Key Libraries:** `com.google.mlkit:text-recognition`, `androidx.activity:activity-ktx` (for result contracts), `androidx.pdf:pdf-viewer` or `android.graphics.pdf.PdfRenderer`.

---

## Stage 10 – Context7 MCP Integration + Hard Rate Limiting + HITL Queue

**New packages** `com.personai.mcp` + `com.personai.ratelimit` + `com.personai.hitl`

**Files**  
- Context7McpClient.kt – thin wrapper that talks to the remote Context7 endpoint (`https://mcp.context7.com/mcp`) or the local npx `@upstash/context7-mcp` process if the user has installed it.  
- RateLimitStore.kt – Room table that records every successful `resolve-library-id` / `query-docs` call with timestamp.  
- MonthlyQuotaGuard.kt – before any Context7 call:  
  - count calls in current calendar month;  
  - if < 1000 → proceed;  
  - if ≥ 1000 → enqueue a `HitlApprovalRequest` (id, library, query, timestamp, status=PENDING) and return a special `QuotaExceeded` result to the agent.  
- HitlApprovalService.kt – foreground notification + NotificationListener that waits for user “Approve continuation” action; on approval the queued job is re-injected into the agent’s tool queue.  
- Tool registration: expose two tools to AgentCore:  
  - `context7_resolve_library_id(libraryName, query)`  
  - `context7_query_docs(libraryId, query)`  
  Both tools go through MonthlyQuotaGuard.

**TDD**  
1. `given 999 prior calls this month when one more is made then call succeeds and counter becomes 1000`.  
2. `given 1000 prior calls when another is requested then HitlApprovalRequest is created and agent receives QuotaExceeded`.  
3. `given pending Hitl request when user taps Approve then original tool call is re-executed and result returned to agent`.  
4. Counter resets automatically on the 1st of each month (WorkManager midnight job).

**Safety** – never store the Context7 API key in plain text; use Android Keystore.

---

## Stage 11 – Local Build / Deploy Engine (the critical missing piece)

**Architecture choice (implement C first, keep A/B as future extension)**  

Preferred path (constrained runtime – most realistic on-device):  
```
User prompt
  → AgentCore + CodingAgentPipeline
  → JSON / declarative UI + logic specification
  → Pre-built PersonAI Runtime (a thin Android shell that can host generated Compose / View code + plugins)
  → Instant “install” of the generated app inside the runtime (no full Gradle compile)
```

Fallback / advanced path (local build daemon):  
```
AI Harness APK
      │ localhost IPC (AIDL / Unix socket)
      ▼
Local Build Engine (native executable or second APK)
      ├── minimal JDK / Kotlin compiler (or pre-downloaded SDK)
      ├── Gradle wrapper
      ├── Android SDK platform-tools
      └── produces signed APK → PackageInstaller
```

**New files under `com.personai.build`**  
- BuildSpec.kt (JSON schema the agent emits)  
- ConstrainedRuntimeHost.kt  
- LocalBuildDaemonClient.kt (IPC)  
- ApkInstaller.kt (PackageInstaller session)  
- ProjectGenerator.kt (uses CodingAgentPipeline to emit Kotlin / Compose / XML / Gradle files)  

**TDD**  
1. Given a simple “Hello World dark theme” prompt, the agent produces a valid BuildSpec.  
2. ConstrainedRuntimeHost can load the generated Compose UI and display it.  
3. End-to-end: “Build me a network scanner with dark UI and CSV export” → generated APK appears in Downloads or is installed via PackageInstaller → can be launched.  
4. Battery / size tests: the build engine itself must not drain > 5 % for a typical generation; generated APKs stay under a configurable size limit.

---

## Stage 12 – End-to-End Local Deployment on Physical Phone

1. Assemble a debug / release APK of the entire PersonAI Gallery harness.  
2. Document the exact steps (ADB install, grant NotificationListener, Accessibility, UsageStats, Overlay permissions).  
3. Provide a one-click “Install generated app” button inside the OverlayChatService.  
4. Write an instrumented test that runs on a real device (or emulator with Google Play) and verifies:  
   - model download & load via LiteRT-LM,  
   - agent can answer a simple prompt,  
   - Context7 call is rate-limited correctly,  
   - a generated micro-app can be installed and launched.  
5. Final acceptance criterion: the whole system runs fully offline after the first model + optional Context7 cache download, and the user can say “Build me X” and receive a working APK on the same phone.

---

## Cross-Cutting Requirements (apply to every stage)

- Before starting a stage, Copilot must first list the files that will be created and the tests that will be written.  
- After each stage, run the full existing Gallery test suite + the new stage’s tests; report any regressions (there must be zero).  
- Frontend changes are only additive (new Fragments / Activities / Compose screens); never alter original Gallery UI entry points.  
- All WorkManager tasks declare `setRequiresDeviceIdle(true)` and `setRequiresBatteryNotLow(true)` where possible.  
- All background inference and embedding batch tasks route through `AdaptiveWorkQueue` and `CpuPacingGovernor` to prevent CPU spikes and thermal throttling.  
- Logging uses a single PersonAILogger that never logs PII.  
- Every tool the agent can call must be declared in a central ToolRegistry with a JSON schema so FunctionGemma can discover it.  
- The coding-agents five-stage pipeline is the **only** permitted way to perform multi-file edits inside a generated project.  
- Context7 is the **only** permitted external documentation source; any other web lookup is forbidden unless the user explicitly enables a “research mode” that itself is rate-limited.

---

## Global Requirements (Extended)
- **Test-Driven Development:** For every new feature, write a test first. Use `JUnit5`, `MockK`, `Robolectric`, and `Espresso` for frontend integration tests. Every new UI component (overlay, wiki graph, PDF viewer, task suggestion card) must have at least one integration test.
- **Context7 MCP:** Before writing code for any new library (e.g., `FunctionGemma`, `sqlite-vector`, `PdfRenderer`), use the `context7` tool to fetch the latest documentation and ensure the code is correct.
- **Privacy & Performance:** All inference (persona, memory, quantum reasoning, Gemma PDF processing) must be on‑device. All background tasks must be constrained (`WorkManager` with idle/battery conditions and `AdaptiveWorkQueue` pacing). Target <2% battery drain per day. The matching system must never expose plain‑text user data; hash all identifiers.
- **Package Isolation:** Add new packages strictly as described (`com.personai.agent`, `com.personai.memory`, `com.personai.learning`, `com.personai.task`, `com.personai.queue`, `com.personai.match`, `com.personai.quantum`, `com.personai.wiki`, `com.personai.pdfreasoner`). No existing Google package may be altered.

## Executing the Prompt
To implement each stage, deploy agent: "Based on the existing `personai-gallery` structure (which remains unmodified), implement `Stage X` following the TDD steps detailed in the prompt."<br>
Note: Before each stage just retrospect what we implemented by combining Stage 0 and after each stage, need to evaluate the changes required in frontend.
