# Multi-Stage TDD Prompt for GitHub Copilot: Extending personai-gallery with AI Persona, Memory System, Social Matching, Quantum-Inspired Reasoning, Deep Wiki, and PDF Reasoning

`.github/copilot-instruction.md`  
**Context:** You are working on a fork of the Google AI Edge Gallery. The primary goal is to develop a personalized AI gallery. The absolute rule is: **do not modify or break any existing files**. You will exclusively **add new files** and extend functionalities by creating new modules that integrate seamlessly. For the frontend you are free to choose architectural decisions, provided every new frontend component is covered by **test‑driven integration tests** that validate the interaction with the new modules.

---

## Stage 0: Infrastructure & Analysis (No Code Changes)
1.  **Read the Repo Structure:** Before any code generation, analyze the existing Android project structure (`/Android/src/`). Familiarize yourself with the existing modules (e.g., `com.google.ai.edge.gallery` package) and `AndroidManifest.xml`.
2.  **Plan New Packages:** All new code will be placed in new packages like `com.personai.agent`, `com.personai.memory`, `com.personai.persona`, `com.personai.notification`, `com.personai.overlay`, `com.personai.sync`, `com.personai.match`, `com.personai.quantum`, `com.personai.wiki`, `com.personai.pdfreasoner`. This ensures zero interference with the original `com.google.ai.edge.gallery` code.

## Stage 1: Contextual Personas Engine (Notification Scraping)
**Feature:** Build dynamic personas(for both user and others) by silently scraping user notifications and caching and processing at intervals.(Allow user the option to choose the people to track and also cross relate across sm platforms)
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

---

## Stage 6: Write-Time Matching System (O(1) Persona Connections)
**Feature:** Instant, privacy‑first matching when two users express mutual interest. The system is designed for O(1) per action: when User A likes User B, it instantly checks if B already liked A; if yes a match is created, otherwise the directed like is stored. No heavy processing on reveal day, and all matching logic runs on‑device with anonymised identifiers.

**New Files:**
- `MatchGraphRepository.kt` (Room entity for directed likes, match‑pairs)
- `MatchService.kt` (Business logic for O(1) write‑time matching)
- `PrivacyHasher.kt` (One‑way hashing of user identifiers to preserve anonymity)

**Implementation Steps (TDD):**
1.  **Write Test:** `given user A likes user B and B has not liked A when like stored then match table remains empty`.
2.  **Implement:** `MatchService.recordLike(fromHash, toHash)` will insert a `Like` row if not exists. After insertion it queries for a reciprocal like: `SELECT 1 FROM likes WHERE fromHash = toHash AND toHash = fromHash`. If found, a `Match` row is created. All queries are indexed and run in O(1) effective time.
3.  **Write Test:** `given both users have liked each other when reciprocal like processed then match created and both parties notified`.
4.  **Implement:** The `MatchService` uses `Room` with appropriate indices `(fromHash, toHash)` and a unique constraint. Notifications are sent via `PersonaNotificationService` to the matched user’s gallery.
5.  **Write Test:** `given anonymous identifiers when stored then no plain‑text email exposed`.
6.  **Implement:** `PrivacyHasher` uses SHA‑256 with a per‑installation salt to convert user‑identifying attributes (e.g., college email) into irreversible hashes, so the local database never contains raw PII.
7.  **Key Libraries:** `androidx.room:room-ktx`, `com.google.crypto.tink:tink-android`.

## Stage 7: Quantum-Inspired Deep Reasoning Engine
**Feature:** A reasoning layer that treats persona snapshots, memory vectors, and activity states as quantum‑like states – superposition, state flipping, and graph rotations – to obtain multi‑dimensional summaries and drive deeper context understanding. This engine enriches the agent’s decisions and the wiki’s automatic linking.

**New Files:**
- `QubitPersonaState.kt` (Encodes persona as a complex probability amplitude vector)
- `QuantumReasoningEngine.kt` (Applies unitary transformations, measurement, and graph rotations)
- `SuperpositionGraph.kt` (Manages relationships between multiple simultaneous states)
- `DimensionalSummarizer.kt` (Projects high‑dimensional qubit states into human‑interpretable summaries)

**Implementation Steps (TDD):**
1.  **Write Test:** `given work and leisure persona dimensions when superposition created then state vector has equal amplitudes for both`.
2.  **Implement:** `QubitPersonaState` uses a small fixed number of basis states (e.g., 8) corresponding to dominant persona modes. Amplitudes are stored as two `FloatArray` (real and imaginary parts). Operations like `superpose(stateA, stateB, alpha)` compute new amplitudes.
3.  **Write Test:** `given a state with work amplitude 0.8 when a flip operation applied then work amplitude becomes 0.8 * -1 (phase flip)`.
4.  **Implement:** `QuantumReasoningEngine.applyFlip(basisIndex)` multiplies the amplitude of that basis by -1. Graph rotations are implemented as a unitary matrix multiplication (e.g., rotation on a pair of basis states). All operations are performed on‑device with Kotlin’s `kotlin.math` or a tiny `Eigen`‑like library.
5.  **Write Test:** `given memory embedding and quantum state when graph rotation performed then nearest links realigned`.
6.  **Implement:** `SuperpositionGraph` represents each wiki node as a qubit; rotations adjust the correlation strength between nodes, enabling emergent deep reasoning connections. `DimensionalSummarizer` performs a projective measurement (collapses the state) to produce a deterministic summary for the UI.
7.  **Key Libraries:** `org.apache.commons:commons-math3` (for complex number support), possibly `com.github.nicklaus4:complexkt` – prefer Kotlin native complex implementations to minimise dependencies. All logic kept under `com.personai.quantum`.

## Stage 8: Deep Wiki with Bidirectional Links (The Memory Core)
**Feature:** A fully local, graph‑based wiki that automatically links memories, files, prompts, persona snapshots, and PDF‑indexed chunks. Users can view the graph, create new links, and update relationships. The wiki becomes the central browsing interface for the user’s knowledge, replacing the simple file list with a visual network. Allow the user to link the photos or videos as memories and use light weight index and reid mechanism and body language assessment capabilities (simplified and user-driven)

**New Files:**
- `WikiGraphDatabase.kt` (Room entities for nodes and edges, plus graph queries)
- `WikiAutoLinker.kt` (Heuristic and quantum‑engine‑driven automatic link creation)
- `WikiFrontendFragment.kt` (Interactive web view or Canvas‑based frontend for exploring the graph)
- `WikiEditorActivity.kt` (Allows manual addition/deletion of links)

**Implementation Steps (TDD):**
1.  **Write Test:** `given a new memory node when auto‑linker runs then edges created to top‑k similar nodes`.
2.  **Implement:** `WikiGraphDatabase` uses three tables: `wiki_nodes`, `wiki_edges`, and `wiki_link_log`. Each node has a type (MEMORY, FILE, PROMPT, PERSONA_SNAPSHOT, PDF_CHUNK) and a JSON payload. Edges store a weight and a `link_type` (AUTO, MANUAL). `WikiAutoLinker` calls `MemoryRepository.similarity_search()` and also uses the quantum engine’s superposition graph to propose non‑obvious links.
3.  **Write Test:** `when user manually links two nodes then edge appears and auto‑link weight adjusted`.
4.  **Implement:** `WikiEditorActivity` exposes a searchable node picker. Manual links are stored with `link_type = MANUAL`. The `WikiFrontendFragment` renders nodes and edges using a `WebView` with a JavaScript graph library (e.g., `vis‑network`) served from local assets, and communicates via a `@JavascriptInterface`. Integration tests will verify that tapping a node triggers the correct gallery action.
5.  **Write Test:** `given PDF chunk node when tapped then PDF viewer opens at the chunk’s page`.
6.  **Implement:** Nodes carry a `referenceURI` (e.g., a content URI to the PDF with page fragment). The frontend dispatches an intent to open the PDF at the indexed location.
7.  **Key Libraries:** `androidx.webkit:webkit`, `com.google.code.gson:gson` (for node metadata). Frontend tests use `Espresso` and `WebView` interactions.

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
1.  **Write Test:** `given a valid PDF URI when parsed with PdfRenderer then page count is correctly determined`.
2.  **Implement:** `PDFPickerHandler` uses `ActivityResultContracts.OpenDocument` to let the user pick a file; it filters for `application/pdf`. On success it opens an `InputStream` to pass to `PdfRenderer`. `PageRangeSelectorDialog` shows the page count and lets the user choose a range.
3.  **Write Test:** `given a page range 2-4 when renderer called then bitmap for page 3 is returned with correct dimensions`.
4.  **Implement:** `PDFPageRenderer` uses Android’s `PdfRenderer` (`android.graphics.pdf.PdfRenderer`) to render each page in the selected range to a `Bitmap`. The renderer is light‑weight and runs entirely on‑device.
5.  **Write Test:** `given a rendered page bitmap when OCR runs then text extracted with accuracy > 90%`.
6.  **Implement:** `OCRTextExtractor` uses `com.google.mlkit:text-recognition` with the latest Latin text recogniser. It returns the raw string.
7.  **Write Test:** `given extracted page text when Gemma reasoning requested then summary JSON returned with topics, entities, and relationships`.
8.  **Implement:** `GemmaReasoningClient` loads the same `FunctionGemma 270M` model as the agent, prompted to output a structured JSON with keys like `summary`, `topics`, `entities`, and `relationships`. All computation stays on‑device.
9.  **Write Test:** `when PDF indexing completes then a wiki node of type PDF_CHUNK created per page with correct page metadata and auto‑links to adjacent pages`.
10. **Implement:** `PDFIndexer` iterates over the selected page range, calls render/OCR/Gemma for each page, then inserts a `wiki_node` of type `PDF_CHUNK` into the `WikiGraphDatabase`. The node stores the content URI of the original PDF, the page number, the OCR text, and the Gemma‑generated summary. Auto‑links are created between consecutive pages and to any relevant existing memory nodes (via similarity search).
11. **Write Test:** `when PDF chunk node tapped in wiki then PDF viewer opens at the exact page`.
12. **Implement:** The node’s `referenceURI` is a content URI with a fragment `?page=3`. The wiki frontend opens an `Intent` with that URI; a dedicated PDF viewer activity (or an external viewer) interprets the fragment to scroll to the specified page. No data leaves the device.
13. **Key Libraries:** `com.google.mlkit:text-recognition`, `androidx.activity:activity-ktx` (for result contracts), `androidx.pdf:pdf-viewer` or `android.graphics.pdf.PdfRenderer`.

---

## Global Requirements (Extended)
- **Test-Driven Development:** For every new feature, write a test first. Use `JUnit5`, `MockK`, `Robolectric`, and `Espresso` for frontend integration tests. Every new UI component (overlay, wiki graph, PDF viewer) must have at least one integration test.
- **Context7 MCP:** Before writing code for any new library (e.g., `FunctionGemma`, `sqlite-vector`, `PdfRenderer`), use the `context7` tool to fetch the latest documentation and ensure the code is correct.
- **Privacy & Performance:** All inference (persona, memory, quantum reasoning, Gemma PDF processing) must be on‑device. All background tasks must be constrained (`WorkManager` with idle/battery conditions). Target <2% battery drain per day. The matching system must never expose plain‑text user data; hash all identifiers.
- **Package Isolation:** Add new packages strictly as described (`com.personai.match`, `com.personai.quantum`, `com.personai.wiki`, `com.personai.pdfreasoner`). No existing Google package may be altered.

## Executing the Prompt
To implement each stage, deploy agent: "Based on the existing `personai-gallery` structure (which remains unmodified), implement `Stage X` following the TDD steps detailed in the prompt."
