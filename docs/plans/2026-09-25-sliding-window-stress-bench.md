# Sliding Window Vectorization, Multi-Scenario & Memory Stress Benchmark Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement sliding window vectorization for long text retrieval without missing localized context, add multi-scenario vector test suites, optimize primitive embedding memory footprint (reducing boxing/GC churn), and introduce an automated memory stress benchmark.

**Architecture:** 
1. **Sliding Window Vectorizer:** `MemoryVectorizer` token-level sliding window (`windowSize = 32`, `stepSize = 16`) producing `TextWindow` vectors with `maxWindowSimilarity` and `bestMatchingWindow` snippet locator.
2. **Repository Substring & Window Retrieval:** `MemoryRepository.searchSimilarSlidingWindow` scoring memories by both global and localized window cosine similarity, returning matched snippet.
3. **Memory Footprint Optimization:** Streamlined vector serialization, avoiding unboxed/boxed collection thrashing during batch evaluation.
4. **Stress & Multi-Scenario Test Harness:** Comprehensive unit and heap-pressure test cases validating polyglot, edge dimensions, long documents, and 1,000+ item query latency/allocation.

**Tech Stack:** Kotlin, AndroidX Room 2.7.0, Coroutines, JUnit 4, Gradle.

---

### Task 1: Sliding Window Vectorization in `MemoryVectorizer`

**Files:**
- Modify: `Android/src/app/src/main/java/com/personai/memory/MemoryVectorizer.kt`
- Test: `Android/src/app/src/test/java/com/personai/memory/MemoryVectorizerTest.kt`

**Step 1: Write failing tests for sliding window vectorization**
Add `slidingWindowGeneratesOverlappingWindows`, `slidingWindowDetectsLocalizedNeedleInHaystack`, and `edgeCaseEmptyOrShortTextReturnsSingleOrEmptyWindow`.

**Step 2: Run test to verify it fails**
Run: `.\gradlew.bat testDebugUnitTest --tests "com.personai.memory.MemoryVectorizerTest"`
Expected: FAIL (unresolved reference: `TextWindow`, `vectorizeSlidingWindow`, `bestMatchingWindow`).

**Step 3: Implement minimal code in `MemoryVectorizer.kt`**
Define:
- `data class TextWindow(val index: Int, val text: String, val vector: FloatArray)`
- `fun vectorizeSlidingWindow(text: String, windowSize: Int = 32, stepSize: Int = 16): List<TextWindow>`
- `fun bestMatchingWindow(queryVector: FloatArray, windows: List<TextWindow>): Pair<TextWindow, Float>?`

**Step 4: Run test to verify it passes**
Run: `.\gradlew.bat testDebugUnitTest --tests "com.personai.memory.MemoryVectorizerTest"`
Expected: PASS

---

### Task 2: Multi-Scenario Semantic Vector Testing

**Files:**
- Modify: `Android/src/app/src/test/java/com/personai/memory/MemoryVectorizerTest.kt`

**Step 1: Add multi-scenario test suite**
- Multi-lingual / Unicode tokenization (Chinese, Japanese, Devanagari, Emoji, accents).
- High-noise input (lots of punctuation, repeated spaces, mixed alphanumeric).
- Extreme lengths (10,000 words document split across windows).
- Orthogonal / opposite vector similarity checks.
- Dimension mismatch safety.

**Step 2: Run tests to verify pass**
Run: `.\gradlew.bat testDebugUnitTest --tests "com.personai.memory.MemoryVectorizerTest"`
Expected: PASS

---

### Task 3: Sliding Window Search in `MemoryRepository`

**Files:**
- Modify: `Android/src/app/src/main/java/com/personai/memory/MemoryRepository.kt`
- Modify: `Android/src/app/src/test/java/com/personai/memory/MemoryRepositoryTest.kt`

**Step 1: Write failing test in `MemoryRepositoryTest.kt`**
Add `searchSimilarSlidingWindowFindsSubstrInLargeDoc` and verify needle retrieval in large haystack memory item.

**Step 2: Run test to verify it fails**
Run: `.\gradlew.bat testDebugUnitTest --tests "com.personai.memory.MemoryRepositoryTest.searchSimilarSlidingWindowFindsSubstrInLargeDoc"`
Expected: FAIL

**Step 3: Implement minimal code in `MemoryRepository.kt`**
Update `ScoredMemoryItem` with `val matchedSnippet: String? = null`.
Implement:
```kotlin
suspend fun searchSimilarSlidingWindow(
    query: String,
    threshold: Float = 0.4f,
    limit: Int = 5,
    maxCandidates: Int = 500,
    windowSize: Int = 32,
    stepSize: Int = 16
): List<ScoredMemoryItem>
```

**Step 4: Run test to verify it passes**
Run: `.\gradlew.bat testDebugUnitTest --tests "com.personai.memory.MemoryRepositoryTest"`
Expected: PASS

---

### Task 4: Memory Usage & Stress Benchmark Suite

**Files:**
- Create: `Android/src/app/src/test/java/com/personai/memory/MemoryStressBenchmarkTest.kt`

**Step 1: Write stress test verifying throughput and heap bounds**
- Ingestion bench: 2,000 memories inserted into repository, tracking elapsed time and heap allocation.
- Query bench: 100 consecutive vector searches across candidate corpus with windowing, asserting average search latency < 50ms on JVM.
- Memory leak / churn test: Ensure no unbounded accumulation of references during continuous sliding window scans.

**Step 2: Run stress benchmark**
Run: `.\gradlew.bat testDebugUnitTest --tests "com.personai.memory.MemoryStressBenchmarkTest"`
Expected: PASS within budget.

---

### Task 5: Audit & Hardening via Cavecrew Reviewer
Run 7-dimension audit with `cavecrew-reviewer`, verify clean PLFS score, and commit with AI attribution.
