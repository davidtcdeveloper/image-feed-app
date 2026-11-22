# Known Issues & Knowledge Evolution Queue

This file captures systemic issues, documentation drift, skill gaps, build/tooling traps, and unresolved inconsistencies discovered during engineering sessions.

## Workflow (Two-Stage Loop)

1. **Post-Session Capture (End of Session / Pre-Commit)**: Agents log newly discovered systemic or unresolved issues below via [`.agents/skills/session-issue-capture/SKILL.md`](../.agents/skills/session-issue-capture/SKILL.md) using the `ISS-NNN` template. Do *not* log transient errors already resolved within the same session.
2. **Manual On-Demand Evaluation**: **Never runs automatically.** Run manually by asking an agent to evaluate `[OPEN]` issues using [`.agents/skills/issue-log-resolution/SKILL.md`](../.agents/skills/issue-log-resolution/SKILL.md).

### Entry Template
```markdown
### [OPEN] ISS-NNN: <Short Title>
- **Date / Context**: YYYY-MM-DD (<spec or task>)
- **Category**: `wiki-drift` | `skill-gap` | `code-inconsistency` | `tooling-friction` | `verification-gap`
- **Symptom & Evidence**: <What conflicted or failed, with file/symbol/command links>
- **Suggested Layer**: `wiki/` | `.agents/skills/` | `AGENTS.md` | `specs/`
```

---

## Open Issues

*(None currently open.)*

---

## Dismissed Issues

*(None yet — intentional trade-offs or rejected proposals closed as `[DISMISSED]` with an explicit rationale are recorded here so future sessions do not re-flag them.)*

---

## Resolved Issues

### [RESOLVED] ISS-001: Pre-Modularization Paths and Absolute Links in Skills
- **Date / Context**: 2026-11-22 (Issue Log Resolution — originally logged 2026-11-21 in Spec 75)
- **Category**: `skill-gap`
- **Symptom & Evidence**: Pre-modularization paths (`shared/src/commonTest`, `shared/commonMain`, `FakeUnsplashRepository`) in `.agents/skills/kmp-testing/SKILL.md` (and remaining `shared/` references in `kmp-architecture` and `verification-and-linting`), plus machine-specific `file:///Users/davidtiagoconceicao/...` links in `.agents/skills/localization/SKILL.md` and `.agents/skills/ios-glass-design/SKILL.md`.
- **Resolution**: Updated [`.agents/skills/kmp-testing/SKILL.md`](../.agents/skills/kmp-testing/SKILL.md), [`.agents/skills/kmp-architecture/SKILL.md`](../.agents/skills/kmp-architecture/SKILL.md), and [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md) to reference modular `:core:*`, `:feature:*`, and `:shared` test/source layout and `./gradlew allTests`; converted all absolute `file:///` links in [`.agents/skills/localization/SKILL.md`](../.agents/skills/localization/SKILL.md) and [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md) to repository-relative paths.

### [RESOLVED] ISS-002: Stale Monolithic Diagram & Tense in `README.md`
- **Date / Context**: 2026-11-22 (Issue Log Resolution — originally logged 2026-11-21 in Spec 75)
- **Category**: `wiki-drift`
- **Symptom & Evidence**: [`README.md`](../README.md) contained the pre-Spec 46 monolithic ASCII diagram (`UnsplashRepository` / `UnsplashApiClient` inside `shared`) and described modularization as currently "executing".
- **Resolution**: Removed the monolithic ASCII diagram in [`README.md`](../README.md), aligned the layer summary and Mermaid graph with the completed modular architecture (including `:core:database` and `:feature:favorites` from Spec 76), updated prose tense to completed state, and linked to [`wiki/architecture-and-modules.md`](architecture-and-modules.md) as the single canonical owner.

### [RESOLVED] ISS-003: Legacy Model `typealias` Shims in `:shared`
- **Date / Context**: 2026-11-22 (Issue Log Resolution — originally logged 2026-11-21 in Spec 75)
- **Category**: `code-inconsistency`
- **Symptom & Evidence**: `shared/src/commonMain/kotlin/com/example/imagefeed/model/PhotoStats.kt` and `shared/src/commonMain/kotlin/com/example/imagefeed/model/UserStats.kt` remained as compatibility `typealias` shims with [`UserProfileScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L123) still importing `com.example.imagefeed.model.UserStats`.
- **Resolution**: Implemented and archived in [`specs/archive/77_shim_retirement_and_swift6_warning_cleanup.md`](../specs/archive/77_shim_retirement_and_swift6_warning_cleanup.md); migrated `UserProfileScreen.kt` to `com.example.imagefeed.profile.model.UserStats` and deleted both shim files.

### [RESOLVED] ISS-004: Worktree Builds Fail Without Exported `ANDROID_HOME` or Local SDK Config
- **Date / Context**: 2026-11-22 (Issue Log Resolution — originally logged 2026-11-21 in Spec 76)
- **Category**: `tooling-friction`
- **Symptom & Evidence**: Fresh git worktrees omit git-ignored `local.properties`, causing `./gradlew` and `xcodebuild` (`:shared:embedAndSignAppleFrameworkForXcode` in [`iosApp/project.yml`](../iosApp/project.yml)) to fail with `SDK location not found` unless `ANDROID_HOME` is exported.
- **Resolution**: Added a `Worktree & Local SDK Configuration` section to [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md) documenting the `ANDROID_HOME` environment fallback alongside `local.properties`.

### [RESOLVED] ISS-005: Pre-Existing Swift 6 Actor-Isolation & Localization Deprecation Warnings
- **Date / Context**: 2026-11-22 (Issue Log Resolution — originally logged 2026-11-21 in Spec 76)
- **Category**: `code-inconsistency`
- **Symptom & Evidence**: Building `iosApp` and `macosApp` emitted pre-existing warnings in [`KFImage.swift`](../iosApp/iosApp/KFImage.swift#L193) (`Placeholder` `#IsolatedConformances` and main-actor static `downsample(data:maxPixelSize:)` call) and [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift#L385) (`appendInterpolation` deprecation warning).
- **Resolution**: Implemented and archived in [`specs/archive/77_shim_retirement_and_swift6_warning_cleanup.md`](../specs/archive/77_shim_retirement_and_swift6_warning_cleanup.md); extracted `ImageDownsampler.downsample` in `KFImage.swift` and updated `tabTitle`/`tabAccessibilityLabel` in `UserProfileView.swift`, restoring zero Swift compiler warnings on `iosApp` and `macosApp`.
