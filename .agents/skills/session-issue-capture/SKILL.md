---
name: session-issue-capture
description: >-
  Post-session capture of systemic conflicts, documentation drift, skill gaps,
  code inconsistencies, and tooling friction into wiki/issue-log.md.
---

# Post-Session Issue Capture Runbook

## Purpose & Triggers

This skill records newly discovered project friction before a work session ends or before changes are committed, keeping [`wiki/issue-log.md`](../../../wiki/issue-log.md) actionable without derailing active feature work.
Use this skill:
- At the end of a development session or during pre-commit review whenever contradictions, documentation drift, stale skill paths, tooling traps, or deferred bugs were discovered.

This skill **only captures** issues. Evaluation and resolution happen separately and manually via [`issue-log-resolution`](../issue-log-resolution/SKILL.md).

---

## 1. Capture Filter (Signal vs. Noise)

Only log issues that are **systemic, recurring, or unresolved**:
- **DO log**:
  - `wiki-drift`: Contradictions between `wiki/*.md`, `README.md`, and the actual codebase.
  - `skill-gap`: Missing, outdated, or misleading instructions or paths in `.agents/skills/*/SKILL.md`.
  - `code-inconsistency`: Architectural boundary violations, legacy shims, or deferred defects discovered out of scope.
  - `tooling-friction`: Repeated Gradle, XcodeGen, `xcodebuild`, Metro, or linter traps not yet documented.
  - `verification-gap`: Systemic or recurring blind spots surfaced in spec `Unverified / Gaps` records.
- **DO NOT log**:
  - Transient typos, syntax errors, or missing imports introduced and resolved within the same session.
  - Spec-specific tasks already tracked inside an active `specs/NN_*.md` document.
  - Items already closed under `## Dismissed Issues` in [`wiki/issue-log.md`](../../../wiki/issue-log.md) unless underlying circumstances changed.

---

## 2. Logging Protocol

1. Check both `## Open Issues` and `## Dismissed Issues` in [`wiki/issue-log.md`](../../../wiki/issue-log.md) to avoid duplicates or re-flagging intentional trade-offs; append new evidence to an existing `[OPEN]` entry if the root cause matches.
2. Assign the next sequential `ISS-NNN` ID under `## Open Issues` using the entry template in [`wiki/issue-log.md`](../../../wiki/issue-log.md).
3. Include exact file paths, symbol names, or failing commands observed in the session so manual evaluation starts with verified evidence.
4. Keep entries concise and adhere to the Living Code & Anti-Staleness Policy (reference files rather than pasting code snippets).
