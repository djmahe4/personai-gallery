# Stage 2: Memory System with File-Prompt Linking & Ebbinghaus Decay Implementation Plan

## Goal
Implement **Stage 2** (Memory System with Vector Representation & File-Prompt Linking) and **Stage 2.1** (Ebbinghaus Memory Decay & Spaced Concept Revision Engine) under `com.personai.memory` with zero edits to `com.google.ai.edge.gallery`.

---

## Architecture & System Design

```text
User File / Prompt / Event
        │
        ▼
[FilePromptLinker] ─────────────► [MemoryVectorizer] (Embedding generation + Cosine similarity)
        │                                  │
        ▼                                  ▼
[MemoryRepository] ─────────────► [MemoryDatabase] (Room with Vector/TypeConverters)
        ▲                                  │
        │                                  ▼
[ConceptRevisionWorker] ◄──── [SpacedRepetitionScheduler] ◄── [EbbinghausDecayEngine]
   (Daily/Periodic Work)        (SM-2 / Leitner Intervals)     (R = e^(-t/S) retention score)
```

### Components
1. **`MemoryVectorizer`**: Generates dense feature vectors for text (TF-IDF/n-gram hashing or LiteRT embedding bridge) and computes vector similarity (cosine similarity).
2. **`MemoryDatabase` & `MemoryEntities`**: Room DB storing `MemoryItem` entities (prompt text, file URI/path, vector embedding blob/floats, tags, timestamp, repetition intervals, stability `S`, retention score `R`).
3. **`MemoryRepository`**: Data access abstraction for insert, query, KNN semantic search by vector similarity threshold, retrieval by file link or concept tag.
4. **`FilePromptLinker`**: Links local document files (URIs, paths, snippets) with prompts and conversation context, storing reciprocal associations in memory.
5. **`EbbinghausDecayEngine`**: Implements $R = e^{-\Delta t / S}$ memory retention decay, updating item retention over elapsed time $\Delta t$ given memory strength/stability $S$.
6. **`SpacedRepetitionScheduler`**: Schedules spaced repetition review intervals using Leitner / SuperMemo (SM-2) adaptation; calculates next review timestamp and updates stability factors on recall success.
7. **`ConceptRevisionWorker`**: Android `CoroutineWorker` running periodic background passes to run decay updates, identify concepts due for revision ($R < R_{threshold}$), and trigger revision queues.

---

## Proposed Changes & File Layout

All code in `Android/src/app/src/main/java/com/personai/memory/` and tests in `Android/src/app/src/test/java/com/personai/memory/`.

### Source Files to Create:
- `Android/src/app/src/main/java/com/personai/memory/MemoryVectorizer.kt`
- `Android/src/app/src/main/java/com/personai/memory/MemoryDatabase.kt`
- `Android/src/app/src/main/java/com/personai/memory/MemoryRepository.kt`
- `Android/src/app/src/main/java/com/personai/memory/FilePromptLinker.kt`
- `Android/src/app/src/main/java/com/personai/memory/EbbinghausDecayEngine.kt`
- `Android/src/app/src/main/java/com/personai/memory/SpacedRepetitionScheduler.kt`
- `Android/src/app/src/main/java/com/personai/memory/ConceptRevisionWorker.kt`

### Test Files to Create:
- `Android/src/app/src/test/java/com/personai/memory/MemoryVectorizerTest.kt`
- `Android/src/app/src/test/java/com/personai/memory/MemoryRepositoryTest.kt`
- `Android/src/app/src/test/java/com/personai/memory/FilePromptLinkerTest.kt`
- `Android/src/app/src/test/java/com/personai/memory/EbbinghausDecayEngineTest.kt`
- `Android/src/app/src/test/java/com/personai/memory/SpacedRepetitionSchedulerTest.kt`

---

## Detailed Implementation Steps

### Phase 1: Vector Representation & Vectorizer
- **File**: `MemoryVectorizer.kt`
- **Responsibilities**:
  - Produce deterministic normalized float vector embeddings (`FloatArray`) from input strings using word n-gram feature hashing (dimension $D=64$ or $128$).
  - Cosine similarity: `cosineSimilarity(v1: FloatArray, v2: FloatArray): Float`.
  - Normalization: $L_2$ norm unit scaling.
- **TDD Test**: `MemoryVectorizerTest.kt`
  - Test vector dimension consistency.
  - Test identity cosine similarity = 1.0f.
  - Test orthogonal / dissimilar texts produce low similarity.
  - Test case-insensitivity and punctuation stripping.

### Phase 2: Room Schema & Database
- **File**: `MemoryDatabase.kt`
- **Entities**:
  - `MemoryItemEntity`:
    - `id: Long = 0` (PrimaryKey, autoGenerate = true)
    - `content: String`
    - `embedding: List<Float>` (converted via TypeConverter to/from JSON or ByteArray)
    - `linkedFileUri: String?`
    - `conceptTag: String?`
    - `createdAt: Long`
    - `lastReviewedAt: Long`
    - `stability: Double` (in days or hours, default 1.0)
    - `repetitionCount: Int`
    - `retentionScore: Double`
    - `nextReviewAt: Long`
- **Dao**: `MemoryDao`:
  - `insert(item: MemoryItemEntity): Long`
  - `update(item: MemoryItemEntity)`
  - `getAll(): List<MemoryItemEntity>`
  - `getByFileUri(uri: String): List<MemoryItemEntity>`
  - `getDueForRevision(cutoffTimestamp: Long): List<MemoryItemEntity>`
  - `deleteById(id: Long)`
- **TypeConverters**: List<Float> serialization.

### Phase 3: MemoryRepository
- **File**: `MemoryRepository.kt`
- **Responsibilities**:
  - Memory persistence & retrieval facade.
  - Semantic search: `searchSimilar(query: String, threshold: Float, limit: Int = 5): List<ScoredMemoryItem>`.
  - Links retrieval: `findMemoriesForFile(fileUri: String): List<MemoryItemEntity>`.
  - Revision retrieval: `getPendingRevisions(currentTime: Long): List<MemoryItemEntity>`.
- **TDD Test**: `MemoryRepositoryTest.kt`
  - Mock DAO / in-memory DB.
  - Test semantic search ranking by cosine similarity.
  - Test filtering below similarity threshold.

### Phase 4: File-Prompt Linker
- **File**: `FilePromptLinker.kt`
- **Responsibilities**:
  - Connect user prompt and contextual referenced files (documents, code snippets, images).
  - Extract document snippet summaries or identifiers.
  - Create reciprocal memory entries linking file metadata with prompt context.
  - Method: `linkPromptToFile(prompt: String, fileUri: String, conceptTag: String?): MemoryItemEntity`.
  - Method: `resolveContextForFile(fileUri: String): List<String>`.
- **TDD Test**: `FilePromptLinkerTest.kt`
  - Verify linked item formation with vector embedding and file URI.
  - Test prompt association retrieval.

### Phase 5: Ebbinghaus Decay Engine
- **File**: `EbbinghausDecayEngine.kt`
- **Formula**:
  $$R = e^{-\frac{\Delta t}{S}}$$
  where $\Delta t$ is elapsed time, $S$ is memory stability (half-life factor).
- **Responsibilities**:
  - Calculate retention $R \in [0.0, 1.0]$ given $\Delta t$ and $S$.
  - Update memory items' retention score based on current clock.
  - Determine if retention has fallen below threshold (e.g. $R < 0.70$).
- **TDD Test**: `EbbinghausDecayEngineTest.kt`
  - Retention at $\Delta t = 0$ is 1.0.
  - Retention decays exponentially with elapsed time.
  - Higher stability $S$ results in slower decay.

### Phase 6: Spaced Repetition Scheduler
- **File**: `SpacedRepetitionScheduler.kt`
- **Algorithm**: Modified SM-2 / Leitner interval spacing.
  - On review evaluation (rating: 1 to 5, or boolean success):
    - If success: $S_{new} = S \times (1 + \text{factor})$, $interval = S_{new}$, repetition count incremented.
    - If failure: $S_{new} = \max(1.0, S \times 0.5)$, repetition count reset, next review immediate.
  - Calculate `nextReviewAt = reviewTime + interval`.
- **TDD Test**: `SpacedRepetitionSchedulerTest.kt`
  - Increasing intervals on consecutive successes.
  - Stability reduction and interval drop on review failure.
  - Accurate calculation of `nextReviewAt` timestamp.

### Phase 7: Concept Revision Worker
- **File**: `ConceptRevisionWorker.kt`
- **Responsibilities**:
  - Extends `CoroutineWorker`.
  - Periodic work trigger (e.g. daily or every 6 hours).
  - Evaluates decaying memories using `EbbinghausDecayEngine`.
  - Flags memories with $R < R_{threshold}$ or `nextReviewAt <= now`.
  - Emits logging and prepares revision set for notification / agent recall.

---

## Verification & Testing Plan

1. **Unit Tests Execution**:
   - Run tests via Gradle: `./gradlew :app:testDebugUnitTest --tests "com.personai.memory.*"`
2. **Scaffolding Invariant**:
   - Ensure `PackageScaffoldingTest` and `com.google.ai.edge.gallery.*` tests remain passing.
   - Verify zero modification in `com.google.ai.edge.gallery` package.
