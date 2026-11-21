# Specifications Directory & Lifecycle

This directory is the **active specification work queue** for the Unsplash Image Feed project. When no features or refactorings are in flight, no numbered specs remain in this root folder.

> [!IMPORTANT]
> **Immutable Archive Policy:** All files inside [`specs/archive/`](archive) (including [`steps_01_to_74.md`](archive/steps_01_to_74.md)) are **final, immutable point-in-time historical records** and must **never** be modified. Current architecture, product/technical business rules, and design standards live exclusively in the **[Project Wiki](../wiki/README.md)**.

---

## Folder Structure

* `specs/NN_<slug>.md` — Active or draft specifications currently in progress.
* [`specs/steps.md`](steps.md) — Compact changelog & archive index mapping every specification ID (`01`..`75+`) to its archived spec and relevant wiki pages.
* [`specs/researches/`](researches) — Exploratory architectural evaluations created prior to formal specifications.
* [`specs/archive/`](archive) — Frozen historical specifications marked `**Status:** Implemented (Final)`.

---

## Specification Lifecycle ("Definition of Done")

1. **Draft / Active (`specs/NN_<name>.md`)**:
   * Every new feature, refactoring, or fix starts as an active spec in `specs/`.
   * Include a **Wiki Impact** section listing which [`wiki/`](../wiki/README.md) pages (if any) will change upon completion.
2. **Implement & Verify**:
   * Execute implementation and pass all platform builds, tests, and linters defined in [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md).
   * Populate a `## Verification Record` section in the spec listing **Verified** (exact commands and observed outcomes) and **Unverified / Gaps** (anything not locally verified and why).
3. **Update Living Wiki & Capture Session Issues**:
   * Update affected [`wiki/*.md`](../wiki/README.md) pages with lasting architectural or business-rule changes.
   * Log any newly discovered out-of-scope inconsistencies, doc drift, recurring verification gaps, or tooling friction in [`wiki/issue-log.md`](../wiki/issue-log.md) per [`.agents/skills/session-issue-capture/SKILL.md`](../.agents/skills/session-issue-capture/SKILL.md).
4. **Finalize & Archive**:
   * Confirm all acceptance criteria checkboxes (`- [x]`) and verify `## Verification Record` is complete.
   * Set spec status to `**Status:** Implemented (Final)`.
   * Move the spec to `specs/archive/NN_<name>.md` and append a 1-line row to the Spec Archive Index in [`specs/steps.md`](steps.md).
