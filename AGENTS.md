# Agent Instructions

## Build System & Commands
Use **Gradle** in `Android/src`:
- Build: `./gradlew assembleDebug` (run within `Android/src/`)
- Test: `./gradlew testDebugUnitTest`

## File-Scoped Commands
| Task | Command |
|------|---------|
| Single Test | `cd Android/src && ./gradlew testDebugUnitTest --tests "com.personai.<package>.<TestClass>"` |
| Module Lint | `cd Android/src && ./gradlew lintDebug` |

## Branch & PR Workflow
- Always branch out: `feat/stage-<N>-<feature-name>` from `copilot/implement-personai-gallery`.
- Submit PR targeting `copilot/implement-personai-gallery`.
- Squash and merge upon passing tests and audit.

## Commit Attribution
AI commits MUST include:
```
Co-Authored-By: (the agent model's name and attribution byline)
```

## Cavecrew Delegation Workflow
Subagent definitions mapped from `.github/skills/`:
- **`cavecrew_investigator`** (`.github/skills/cavecrew/SKILL.md` + `.github/skills/writing-plans/SKILL.md`): Codebase investigation & atomic TDD plan creation (`docs/plans/YYYY-MM-DD-<stage>.md`).
- **`cavecrew_builder`** (`.github/skills/cavecrew/SKILL.md` + `.github/skills/executing-plans/SKILL.md`): Surgical TDD execution (failing test -> minimal code -> pass).
- **`cavecrew_reviewer`** (`.github/skills/cavecrew/SKILL.md` + `.github/skills/vibe-code-auditor/SKILL.md`): 7-dimension production code audit, PLFS scoring, hardening.
- **`caveman`** (`.github/skills/caveman/SKILL.md`): Terse compressed output mode across all interactions.

## Key Architecture Conventions
- **Zero-Modification Upstream Rule**: Never modify files in `com.google.ai.edge.gallery`. Add only new packages under `com.personai.*`.
- **TDD Workflow**: Write failing test first, verify failure, implement minimal code, verify pass.
- **On-Device & Resource-Aware**: All inference runs locally with strict WorkManager constraints (`setRequiresDeviceIdle(true)`, `setRequiresBatteryNotLow(true)`).
- **Additive Frontend UI Rule**: Evaluate and implement additive UI components for every stage (e.g. `PersonaTrackingPreferencesFragment`, `MemoryRevisionDashboardFragment`). Never alter original Gallery navigation routes directly; add tests verifying UI contract.
- **Context7 Auto-Research**: Consult Context7 MCP for new library integration and on-device API patterns before generating implementations.
