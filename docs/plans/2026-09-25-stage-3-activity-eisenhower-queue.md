# Stage 3, Stage 3.1 & Stage 3.2: Activity Analyzer, Eisenhower Matrix & Paced Work Queue Implementation Plan

## Goal
Implement:
1. **Stage 3: Background User Activity Analyzer** (`com.personai.activity` / `com.personai.task`) — Periodic WorkManager activity profiling via `UsageStatsManager`, frequency & routine anomaly detection (`UsagePatternModel`).
2. **Stage 3.1: Eisenhower Matrix Task Prioritization & Smart Suggestion Engine** (`com.personai.task`) — 2x2 matrix classification (`Q1-Q4`), Hick's Law suggestion advisor (<= 3 suggestions), `QuadrantQueueManager` integrating `ConceptRevisionWorker` and agent dispatch, additive `EisenhowerDashboardFragment`.
3. **Stage 3.2: Paced Background Task Queue & Adaptive CPU Throttler** (`com.personai.queue`) — Rate-paced bounded work channel (`concurrency = 1`), `CpuPacingGovernor` with thermal and battery-aware delay, `ThermalBatteryMonitor` (`PowerManager.OnThermalStatusChangedListener` + battery broadcast).

Zero modifications to upstream `com.google.ai.edge.gallery.*`. All PersonAI code strictly in `com.personai.*`.

---

## Architecture & System Design

```text
       [System UsageStatsManager]
                  │
                  ▼
       [ActivityAnalyzerService / Worker] ──► [UsagePatternModel] (Routine & peak usage detection)
                  │
                  ▼
       [EisenhowerTaskClassifier] ◄── [ConceptRevisionWorker] (Q2 Revisions) / [Notification / Prompt Events]
                  │
                  ▼
       [QuadrantQueueManager] (Q1: Do Now, Q2: Schedule & Protect, Q3: Delegate, Q4: Eliminate)
                  │
                  ├──────────────────────────────────┐
                  ▼                                  ▼
       [TaskPriorityAdvisor]             [AdaptiveWorkQueue] (Bounded concurrency channel = 1)
     (Hick's Law <= 3 items)                         │
                  │                                  ▼
                  ▼                      [CpuPacingGovernor] (Dynamic pause / backoff)
       [EisenhowerDashboardFragment]                 ▲
         (Additive UI Contract)                      │
                                         [ThermalBatteryMonitor] (PowerManager / Battery)
```

### Component Details
1. **Stage 3 (Activity Analyzer)**:
   - `UsagePatternModel.kt`: Pure data/math model analyzing usage duration, frequency per package/category, computing routine baseline and anomaly score.
   - `ActivityAnalyzerWorker.kt`: `CoroutineWorker` querying `UsageStatsManager` (or mocked usage provider for tests) under idle/charging constraints, outputting routine focus windows.
2. **Stage 3.1 (Eisenhower Matrix & Task Prioritization)**:
   - `EisenhowerTask.kt`: Task entity/data class with id, title, description, urgency score (0.0-1.0), importance score (0.0-1.0), deadline, quadrant (`Q1_DO_NOW`, `Q2_SCHEDULE`, `Q3_DELEGATE`, `Q4_ELIMINATE`), source (`CONCEPT_REVISION`, `NOTIFICATION`, `USER_PROMPT`, `SYSTEM`).
   - `EisenhowerTaskClassifier.kt`: Maps task attributes (urgency, importance, deadline proximity, retention decay) to quadrants. Spaced concept revisions with decay $R < 0.45$ are routed to `Q2_SCHEDULE`. Repetitive routine queries routed to `Q3_DELEGATE`.
   - `QuadrantQueueManager.kt`: In-memory and Room-backed priority queues for all 4 quadrants; connects Q2 to preferred learning windows from `UsagePatternModel` and auto-routes Q3 tasks to agent delegates.
   - `TaskPriorityAdvisor.kt`: Enforces Hick's Law: returns $\le 3$ highest-leverage recommendations sorted by quadrant priority (Q1 > Q2 > Q3).
   - `TaskSuggestionCard.kt`: Presentation model for suggestion cards with 1-click execution for Q3 delegate actions.
   - `EisenhowerDashboardFragment.kt`: Additive UI Fragment showing quadrant tallies, active suggestions, and action triggers.
3. **Stage 3.2 (Paced Background Task Queue & Resource Governor)**:
   - `ThermalBatteryMonitor.kt`: Listens to `PowerManager.OnThermalStatusChangedListener` (or simulated states) and battery percentage/charging broadcasts.
   - `CpuPacingGovernor.kt`: Computes dynamic pacing delay (e.g. 0ms nominal, 250ms moderate thermal, 1000ms severe thermal, pause if critical or battery < 20% discharging).
   - `AdaptiveWorkQueue.kt`: Priority background task queue with bounded coroutine channel (`concurrency = 1`), pacing injection before/after heavy jobs, and pause/resume lifecycle.

---

## Proposed Changes & File Layout

### Source Files to Create:
- `Android/src/app/src/main/java/com/personai/activity/UsagePatternModel.kt`
- `Android/src/app/src/main/java/com/personai/activity/ActivityAnalyzerWorker.kt`
- `Android/src/app/src/main/java/com/personai/task/EisenhowerTask.kt`
- `Android/src/app/src/main/java/com/personai/task/EisenhowerTaskClassifier.kt`
- `Android/src/app/src/main/java/com/personai/task/QuadrantQueueManager.kt`
- `Android/src/app/src/main/java/com/personai/task/TaskPriorityAdvisor.kt`
- `Android/src/app/src/main/java/com/personai/task/TaskSuggestionCard.kt`
- `Android/src/app/src/main/java/com/personai/task/EisenhowerDashboardFragment.kt`
- `Android/src/app/src/main/java/com/personai/queue/ThermalBatteryMonitor.kt`
- `Android/src/app/src/main/java/com/personai/queue/CpuPacingGovernor.kt`
- `Android/src/app/src/main/java/com/personai/queue/AdaptiveWorkQueue.kt`

### Test Files to Create:
- `Android/src/app/src/test/java/com/personai/activity/UsagePatternModelTest.kt`
- `Android/src/app/src/test/java/com/personai/activity/ActivityAnalyzerWorkerTest.kt`
- `Android/src/app/src/test/java/com/personai/task/EisenhowerTaskClassifierTest.kt`
- `Android/src/app/src/test/java/com/personai/task/TaskPriorityAdvisorTest.kt`
- `Android/src/app/src/test/java/com/personai/task/QuadrantQueueManagerTest.kt`
- `Android/src/app/src/test/java/com/personai/task/EisenhowerDashboardFragmentTest.kt`
- `Android/src/app/src/test/java/com/personai/queue/ThermalBatteryMonitorTest.kt`
- `Android/src/app/src/test/java/com/personai/queue/CpuPacingGovernorTest.kt`
- `Android/src/app/src/test/java/com/personai/queue/AdaptiveWorkQueueTest.kt`

---

## Detailed TDD Implementation Steps

### Phase 1: Stage 3 — Activity Analyzer & Usage Pattern Model
- **Test 1.1**: `UsagePatternModelTest.kt`
  - Test routine detection: identify high-usage peak hours from usage timestamps.
  - Test anomaly detection: flag sudden 3x spike above baseline as anomalous.
  - Test focus window suggestion: compute optimal quiet learning periods based on low user interaction intervals.
- **Implementation 1.1**: `UsagePatternModel.kt`
  - Pure Kotlin class with statistics computation (hourly histogram, mean/std-dev anomaly z-score).
- **Test 1.2**: `ActivityAnalyzerWorkerTest.kt`
  - Test WorkManager constraints builder specifies `requiresDeviceIdle` and `requiresBatteryNotLow`.
  - Test usage stats aggregation and routine extraction.
- **Implementation 1.2**: `ActivityAnalyzerWorker.kt`
  - `CoroutineWorker` querying system usage stats or fallback aggregator, scheduling periodic sync.

### Phase 2: Stage 3.1 — Eisenhower Matrix & Task Prioritization
- **Test 2.1**: `EisenhowerTaskClassifierTest.kt`
  - Test urgent & important alert classified as `Q1_DO_NOW`.
  - Test spaced concept revision ($R < 0.45$) classified as `Q2_SCHEDULE`.
  - Test routine repetitive task classified as `Q3_DELEGATE`.
  - Test low-signal ephemeral notification classified as `Q4_ELIMINATE`.
- **Implementation 2.1**: `EisenhowerTask.kt` & `EisenhowerTaskClassifier.kt`
  - Domain models and classification rules with configurable thresholds.
- **Test 2.2**: `QuadrantQueueManagerTest.kt`
  - Test routing into 4 quadrant queues.
  - Test Q3 auto-delegation hook triggering agent action runner.
  - Test concept revision worker output integration into Q2 queue.
- **Implementation 2.2**: `QuadrantQueueManager.kt`
  - Queue coordination manager maintaining thread-safe quadrant state.
- **Test 2.3**: `TaskPriorityAdvisorTest.kt`
  - Test Hick's Law: advisor strictly returns $\le 3$ suggestions even when dozens of tasks exist across queues.
  - Test prioritization order: Q1 critical items first, followed by Q2 focus revisions.
- **Implementation 2.3**: `TaskPriorityAdvisor.kt` & `TaskSuggestionCard.kt`
  - Recommendation engine producing formatted cards for UI display.
- **Test 2.4**: `EisenhowerDashboardFragmentTest.kt` (Robolectric)
  - Test fragment inflates and displays counts for Q1-Q4.
  - Test top 3 suggestions rendered.
  - Test 1-click execution callback on Q3 suggestion card.
- **Implementation 2.4**: `EisenhowerDashboardFragment.kt`
  - Additive UI Fragment adhering to PersonAI UI contract.

### Phase 3: Stage 3.2 — Paced Background Work Queue & Adaptive CPU Throttler
- **Test 3.1**: `ThermalBatteryMonitorTest.kt`
  - Test thermal status tracking (nominal, moderate, severe, critical).
  - Test battery level and charging state transitions.
- **Implementation 3.1**: `ThermalBatteryMonitor.kt`
  - State holder listening to thermal listener and battery broadcast intents.
- **Test 3.2**: `CpuPacingGovernorTest.kt`
  - Test zero pacing delay on nominal thermal and healthy battery.
  - Test throttle delay injection (e.g. 250ms) on `THERMAL_STATUS_MODERATE`.
  - Test heavy delay / pause recommendation when battery < 20% not charging or thermal status severe/critical.
- **Implementation 3.2**: `CpuPacingGovernor.kt`
  - Pacing policy evaluator calculating inter-job delay and execution feasibility.
- **Test 3.3**: `AdaptiveWorkQueueTest.kt`
  - Test bounded sequential processing (`concurrency = 1`).
  - Test inter-job delay injection governed by `CpuPacingGovernor`.
  - Test pause and resume of non-critical jobs when battery drops below 20%.
  - Test high-priority job bypass vs background batch deferral.
- **Implementation 3.3**: `AdaptiveWorkQueue.kt`
  - Coroutine channel-based pacing queue with lifecycle control (`pause()`, `resume()`, `enqueue()`).

---

## Verification & Test Plan
1. Unit Tests via Gradle:
   `cd Android/src && ./gradlew testDebugUnitTest --tests "com.personai.activity.*" --tests "com.personai.task.*" --tests "com.personai.queue.*"`
2. Verify all existing tests pass:
   `cd Android/src && ./gradlew testDebugUnitTest`
3. Verify zero modifications to upstream `com.google.ai.edge.gallery`.
