# PersonAI Gallery Harness Architecture (Stage 0)

## Goals
- Keep all original Gallery code under `com.google.ai.edge.gallery` untouched.
- Add PersonAI capabilities only through additive modules under `com.personai.*`.
- Run core inference and reasoning on-device.

## High-level flow

```text
User Prompt / Device Signals
        |
        v
+---------------------------+
| PersonAI Agent Core       |
| - ToolRegistry            |
| - CodingAgentPipeline     |
+------------+--------------+
             |
             +--> Persona / Memory / Match / Quantum / Wiki / PDF modules
             |
             +--> Local Build Host / APK Installer
             |
             +--> Context7 MCP Client
                     |
                     v
             +-------------------------+
             | Quota Guard + HITL Queue|
             | - rate-limit store      |
             | - approval notification |
             +-------------------------+
```

## Context7 + HITL controls
- Context7 requests flow through a quota guard and persisted counter store.
- When quota is exceeded, requests are enqueued to a HITL approval queue.
- Agent execution pauses until user/admin approval replays the queued request.

## Public skill references
- Caveman: https://raw.githubusercontent.com/JuliusBrussee/caveman/main/skills/caveman/SKILL.md
- CaveCrew: https://raw.githubusercontent.com/JuliusBrussee/caveman/main/skills/cavecrew/SKILL.md

## Stage 0 package scaffolding
- `com.personai.agent`
- `com.personai.memory`
- `com.personai.persona`
- `com.personai.notification`
- `com.personai.overlay`
- `com.personai.sync`
- `com.personai.match`
- `com.personai.quantum`
- `com.personai.wiki`
- `com.personai.pdfreasoner`
- `com.personai.build`
- `com.personai.mcp`
- `com.personai.ratelimit`
- `com.personai.hitl`
