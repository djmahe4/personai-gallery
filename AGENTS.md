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

## Commit Attribution
AI commits MUST include:
```
Co-Authored-By: (the agent model's name and attribution byline)
```

## Context & Token Efficiency Skills
- **`caveman`** (`.github/skills/caveman/SKILL.md`): Ultra-compressed communication mode. Cuts tokens 65% while keeping exact technical substance.
- **`cavecrew`** (`.github/skills/cavecrew/SKILL.md`): Subagent delegation presets (`cavecrew-investigator`, `cavecrew-builder`, `cavecrew-reviewer`) with compressed tool results to preserve session context.

## Key Architecture Conventions
- **Zero-Modification Upstream Rule**: Never modify files in `com.google.ai.edge.gallery`. Add only new packages under `com.personai.*`.
- **TDD Workflow**: Write failing test first, verify failure, implement minimal code, verify pass.
- **On-Device & Resource-Aware**: All inference runs locally with strict WorkManager constraints (`setRequiresDeviceIdle(true)`, `setRequiresBatteryNotLow(true)`).
