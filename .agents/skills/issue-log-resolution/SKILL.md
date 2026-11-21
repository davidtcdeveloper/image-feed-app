---
name: issue-log-resolution
description: >-
  Manual on-demand evaluation and resolution of open issues in wiki/issue-log.md,
  routing fixes cleanly across wiki/, skills, AGENTS.md, or new specs.
---

# Manual Issue Log Evaluation & Resolution Runbook

## Purpose & Triggers

This skill evaluates accumulated `[OPEN]` entries in [`wiki/issue-log.md`](../../../wiki/issue-log.md), verifies root causes against living source files, and routes fixes to the appropriate repository layer.
- **Manual-Only Execution**: Run this skill **only when explicitly requested by the user** (e.g., *"evaluate open issues in `wiki/issue-log.md`"* or *"run issue-log-resolution"*). **Never schedule or execute this workflow automatically in the background.**

---

## 1. Triage & Root-Cause Verification

1. Read all `[OPEN]` entries in [`wiki/issue-log.md`](../../../wiki/issue-log.md) **and** scan `Unverified / Gaps` sections in recently archived specs under `specs/archive/` for recurring verification blind spots.
2. Inspect the referenced living source files, build scripts, or skill documents to verify whether each symptom still exists and pinpoint its root cause.
3. Group related entries when multiple symptoms share one underlying cause.

---

## 2. Route Each Fix to the Right Layer

Before adding or editing any rule, run an **Anti-Context-Poison (Single Owner per Rule)** check: search `wiki/`, `.agents/skills/`, `README.md`, and `AGENTS.md` to ensure no duplicate or conflicting rule exists elsewhere, and consolidate to a single canonical owner with links rather than repeating prose.

Apply updates cleanly to the single best layer without duplicating guidance:
- **Living System or Business Knowledge (`wiki/*.md`)**:
  - Route product/technical business rules to [`wiki/business-rules.md`](../../../wiki/business-rules.md).
  - Route module, DI, UI, performance, or decision updates to [`wiki/architecture-and-modules.md`](../../../wiki/architecture-and-modules.md), [`wiki/design-systems-and-navigation.md`](../../../wiki/design-systems-and-navigation.md), [`wiki/performance-and-memory.md`](../../../wiki/performance-and-memory.md), or [`wiki/adr-index.md`](../../../wiki/adr-index.md).
- **Procedural Runbooks (`.agents/skills/*/SKILL.md`)**:
  - Update build/lint commands, verification checklists, or coding procedures in the matching skill.
  - Strictly obey the **Living Code & Anti-Staleness Policy**: link to compiler-verified source files instead of embedding multi-line code blocks.
- **Root Invariants ([`AGENTS.md`](../../../AGENTS.md))**:
  - Only modify `AGENTS.md` for non-negotiable repository-wide invariants, keeping total length strictly under **75 lines**.
- **Code Defects or Multi-File Refactors (`specs/NN_*.md`)**:
  - Do **not** mix large code refactors silently into documentation hygiene commits.
  - Either draft a focused active specification in `specs/NN_<issue>.md` (following [`specs/README.md`](../../../specs/README.md)) or implement small scoped fixes separately with full platform verification from [`verification-and-linting`](../verification-and-linting/SKILL.md).

---

## 3. Close Queue Items

1. Never leave resolved or intentionally rejected entries dangling as `[OPEN]`.
2. Close each triaged item cleanly:
   - **`[RESOLVED]`**: Move verified fixes from `## Open Issues` to `## Resolved Issues` in [`wiki/issue-log.md`](../../../wiki/issue-log.md), noting the date and what changed (updated skill, wiki section, or created spec ID).
   - **`[DISMISSED]`**: Move intentional trade-offs or rejected proposals to `## Dismissed Issues` in [`wiki/issue-log.md`](../../../wiki/issue-log.md) with an explicit rationale so future sessions do not re-flag them.
3. Never modify immutable specifications under `specs/archive/`.
