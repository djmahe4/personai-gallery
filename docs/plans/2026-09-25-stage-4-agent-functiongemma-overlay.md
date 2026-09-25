# Stage 4: On-Device AI Agent (FunctionGemma 270M & Overlay Chat Service) Implementation Plan

## Goal
Implement:
1. **`com.personai.agent.AgentCore`**: FunctionGemma 270M function calling schema, tool invocation engine (gallery operations: image search, tag filtering, concept linking, web search dispatch).
2. **`com.personai.agent.UIAutomationHelper`**: Android `AccessibilityService` integration (`AccessibilityNodeInfo`, `ACTION_CLICK`, `ACTION_SCROLL_FORWARD`), element lookup by view ID/text, fallback Google Custom Search URL dispatch.
3. **`com.personai.agent.OverlayChatService`**: `WindowManager` floating chat head overlay (`TYPE_APPLICATION_OVERLAY`), Compose view composition, overlay lifecycle management, and message handling.
4. **`com.personai.agent.AgentDashboardFragment`**: Additive UI / Fragment contract displaying agent status, tool execution history, and accessibility/overlay permission indicators.
5. **Unit tests across all components** using Robolectric and JUnit5 / MockK. Zero edits to `com.google.ai.edge.gallery.*`.

---

## Architecture & System Design

```text
               ┌────────────────────────────────────────────────────────┐
               │              OverlayChatService / UI                   │
               │   (WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) │
               └─────────────────────────┬──────────────────────────────┘
                                         │ User Prompt / Chat Input
                                         ▼
               ┌────────────────────────────────────────────────────────┐
               │                       AgentCore                        │
               │  - FunctionGemma 270M Schema Registry                  │
               │  - Tool Invocation Engine:                             │
               │    * searchImages(query, tag)                          │
               │    * filterByTag(tag)                                  │
               │    * linkMemoryConcept(memoryId, conceptTag)           │
               │    * dispatchWebSearch(query)                          │
               └───────────┬────────────────────────────┬───────────────┘
                           │ Fallback UI / Web Action   │ Gallery Operations
                           ▼                            ▼
        ┌───────────────────────────────────┐    ┌─────────────────────────┐
        │        UIAutomationHelper         │    │  com.personai.memory.*  │
        │ - AccessibilityNodeInfo resolver  │    │  (MemoryRepository,     │
        │ - ACTION_CLICK / ACTION_SCROLL    │    │   FilePromptLinker)     │
        │ - Web Search Intent / Dispatch    │    └─────────────────────────┘
        └───────────────────────────────────┘
                           │
                           ▼
        ┌───────────────────────────────────┐
        │      AgentDashboardFragment       │
        │ - Agent Status & History Feed     │
        │ - Overlay & Accessibility Toggles │
        └───────────────────────────────────┘
```

### Component Details
1. **`AgentCore.kt` (`com.personai.agent`)**:
   - Represents FunctionGemma 270M function calling schema definition: `ToolSchema`, `FunctionCall`, `FunctionResponse`, `ToolExecutionResult`.
   - Tool registry containing 4 built-in gallery operations:
     1. `searchImages(query: String, tag: String?)`: Queries gallery or vector index.
     2. `filterByTag(tag: String)`: Filters memory and item tags.
     3. `linkMemoryConcept(memoryId: Long, conceptTag: String)`: Associates concept node to memory.
     4. `dispatchWebSearch(query: String)`: Delegates web search action to automation engine.
   - Deterministic prompt-to-function parsing harness supporting both LiteRT-LM tool definitions and structured JSON/schema fallback.
2. **`UIAutomationHelper.kt` (`com.personai.agent`)**:
   - Android `AccessibilityService` wrapper (`PersonAIAccessibilityService`) and automation controller.
   - Finds `AccessibilityNodeInfo` by view ID or text.
   - Performs simulated actions: `ACTION_CLICK`, `ACTION_SCROLL_FORWARD`, `ACTION_SET_TEXT`.
   - Fallback Google Search dispatcher generating URI `https://www.google.com/search?q={query}` or invoking `CustomSearchApi` when API key is provided.
3. **`OverlayChatService.kt` (`com.personai.agent`)**:
   - Extends `android.app.Service`.
   - Manages floating chat head via `WindowManager` with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
   - Embeds Compose `ComposeView` for reactive chat interactions on top of any screen.
   - Touch drag listener for moving chat bubble across display edges.
4. **`AgentDashboardFragment.kt` (`com.personai.agent`)**:
   - Additive Fragment following PersonAI design pattern (like `EisenhowerDashboardFragment` and `MemoryRevisionDashboardFragment`).
   - Displays agent status (Idle, Thinking, Executing Tool), recent tool execution history list, permission health checks (Overlay permission `Settings.canDrawOverlays`, Accessibility permission enabled).

---

## Proposed Changes & File Layout

### Source Files to Create:
- `Android/src/app/src/main/java/com/personai/agent/AgentModels.kt` (Function calling schemas, function signatures, execution state)
- `Android/src/app/src/main/java/com/personai/agent/AgentCore.kt` (FunctionGemma 270M engine & tool registry)
- `Android/src/app/src/main/java/com/personai/agent/PersonAIAccessibilityService.kt` (Android AccessibilityService implementation)
- `Android/src/app/src/main/java/com/personai/agent/UIAutomationHelper.kt` (Node lookup, action performer, search dispatch)
- `Android/src/app/src/main/java/com/personai/agent/OverlayChatService.kt` (WindowManager floating chat service)
- `Android/src/app/src/main/java/com/personai/agent/AgentDashboardFragment.kt` (Additive Compose-based UI fragment)

### Manifest & Resource Updates:
- Update `Android/src/app/src/main/AndroidManifest.xml` to declare:
  - `<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />`
  - `<service android:name="com.personai.agent.OverlayChatService" android:exported="false" />`
  - `<service android:name="com.personai.agent.PersonAIAccessibilityService" ...>` with accessibility service config.
- Add accessibility config `Android/src/app/src/main/res/xml/personai_accessibility_service_config.xml`.

### Test Files to Create:
- `Android/src/app/src/test/java/com/personai/agent/AgentCoreTest.kt`
- `Android/src/app/src/test/java/com/personai/agent/UIAutomationHelperTest.kt`
- `Android/src/app/src/test/java/com/personai/agent/OverlayChatServiceTest.kt`
- `Android/src/app/src/test/java/com/personai/agent/AgentDashboardFragmentTest.kt`

---

## Detailed TDD Implementation Steps

### Phase 1: Tool Registry & AgentCore (FunctionGemma 270M)
- **Test 1.1**: `AgentCoreTest.kt`
  - `given natural language query when parsed then correct function selected()`:
    - "find pictures of sunsets" -> selects `searchImages` with `query="sunsets"`.
    - "filter tags by vacation" -> selects `filterByTag` with `tag="vacation"`.
    - "link memory 42 with biology" -> selects `linkMemoryConcept` with `memoryId=42, conceptTag="biology"`.
    - "search google for latest android release" -> selects `dispatchWebSearch` with `query="latest android release"`.
  - Tool execution dispatch: executing registered tools yields structured `ToolExecutionResult.Success` with expected output payload.
  - Unknown function call gracefully returns `ToolExecutionResult.Failure`.
- **Implementation 1.1**:
  - `AgentModels.kt`: Schema metadata, argument maps, `ToolExecutionResult`.
  - `AgentCore.kt`: Tool definitions matching FunctionGemma specifications, schema registry, parser and runner.

### Phase 2: UI Automation Helper & Accessibility Service
- **Test 2.1**: `UIAutomationHelperTest.kt`
  - Element lookup: finds mock `AccessibilityNodeInfo` by view ID or text snippet.
  - Action execution: `performClick` triggers `AccessibilityNodeInfo.ACTION_CLICK`; `performScroll` triggers `ACTION_SCROLL_FORWARD`.
  - Missing element returns failure/false without throwing exceptions.
  - Search fallback: `dispatchWebSearch` creates search intent with query URI `https://www.google.com/search?q=...`.
- **Implementation 2.1**:
  - `PersonAIAccessibilityService.kt`: Accessibility event interceptor and root node provider.
  - `UIAutomationHelper.kt`: Node hierarchy search, action dispatcher, and search intent builder.
  - `personai_accessibility_service_config.xml`: Configure flags (`flagRetrieveInteractiveWindows`, `feedbackGeneric`).

### Phase 3: WindowManager Overlay Chat Service
- **Test 3.1**: `OverlayChatServiceTest.kt` (Robolectric)
  - `when overlay launched then window appears above all activities()`:
    - Service starts, instantiates `WindowManager.LayoutParams` with `TYPE_APPLICATION_OVERLAY`.
    - Floating bubble view is added to `WindowManager`.
    - Chat expansion and collapse state toggling.
    - Stopping service removes overlay view from `WindowManager`.
- **Implementation 3.1**:
  - `OverlayChatService.kt`: Service implementation managing overlay view lifecycle, touch listeners, drag mechanics, and Compose bubble UI.

### Phase 4: Additive Agent Dashboard Fragment
- **Test 4.1**: `AgentDashboardFragmentTest.kt` (Robolectric)
  - Fragment renders agent status indicator ("Idle", "Active").
  - Execution history list displays recent tool invocations.
  - Permission status displays overlay and accessibility grant states.
  - State preservation across bundle restore.
- **Implementation 4.1**:
  - `AgentDashboardFragment.kt`: Compose-based additive UI fragment adhering strictly to zero-modification rule.

---

## Verification & Commands
All tests run with Studio JBR:
```bash
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd Android/src
./gradlew testDebugUnitTest --tests "com.personai.agent.*"
```
Ensure 100% pass across all unit tests and zero diffs to `com.google.ai.edge.gallery.*`.
