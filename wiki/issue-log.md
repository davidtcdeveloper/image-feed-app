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

### [OPEN] ISS-001: Pre-Modularization Paths and Absolute Links in Skills
- **Date / Context**: 2026-11-21 (Spec 75 — Spec Archive & Wiki Migration)
- **Category**: `skill-gap`
- **Symptom & Evidence**:
  * [`.agents/skills/kmp-testing/SKILL.md`](../.agents/skills/kmp-testing/SKILL.md) still references pre-modularization paths (`shared/src/commonTest`, `shared/commonMain`, `FakeUnsplashRepository`, and `shared/src/commonTest/kotlin/com/example/imagefeed/presentation/feed/FeedViewModelTest.kt`) instead of the modularized `:core:*` and `:feature:*` test suites (e.g., [`feature/feed/src/commonTest/kotlin/com/example/imagefeed/presentation/feed/FeedViewModelTest.kt`](../feature/feed/src/commonTest/kotlin/com/example/imagefeed/presentation/feed/FeedViewModelTest.kt)).
  * [`.agents/skills/localization/SKILL.md`](../.agents/skills/localization/SKILL.md) and [`.agents/skills/ios-glass-design/SKILL.md`](../.agents/skills/ios-glass-design/SKILL.md) use machine-specific `file:///Users/davidtiagoconceicao/...` absolute links instead of repository-relative links, which break in worktrees and other clones.
- **Suggested Layer**: `.agents/skills/`

### [OPEN] ISS-002: Stale Monolithic Diagram & Tense in `README.md`
- **Date / Context**: 2026-11-21 (Spec 75 — Spec Archive & Wiki Migration)
- **Category**: `wiki-drift`
- **Symptom & Evidence**: [`README.md`](../README.md#L144-L199) still contains the pre-Spec 46 monolithic ASCII diagram depicting `UnsplashRepository` and `UnsplashApiClient` inside `shared`, and describes the completed modularization migration (Specs 46–49) as currently "executing".
- **Suggested Layer**: `README.md` / `wiki/`

### [OPEN] ISS-003: Legacy Model `typealias` Shims in `:shared`
- **Date / Context**: 2026-11-21 (Spec 75 — Spec Archive & Wiki Migration)
- **Category**: `code-inconsistency`
- **Symptom & Evidence**: [`shared/src/commonMain/kotlin/com/example/imagefeed/model/PhotoStats.kt`](../shared/src/commonMain/kotlin/com/example/imagefeed/model/PhotoStats.kt) and [`shared/src/commonMain/kotlin/com/example/imagefeed/model/UserStats.kt`](../shared/src/commonMain/kotlin/com/example/imagefeed/model/UserStats.kt) remain as backward-compatibility `typealias` shims pointing to `:feature:details` and `:feature:profile`. Only [`UserProfileScreen.kt`](../androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L118) still imports `com.example.imagefeed.model.UserStats`; `PhotoStats` has no remaining Kotlin usages outside Swift interop checks. Evaluate migrating callers to the feature packages and retiring the shims cleanly.
- **Suggested Layer**: `specs/` (or scoped cleanup task)

### [OPEN] ISS-004: Worktree Builds Fail Without Exported `ANDROID_HOME` or Local SDK Config
- **Date / Context**: 2026-11-21 (Spec 76 — Local Favorites Persistence & Favorites Screen)
- **Category**: `tooling-friction`
- **Symptom & Evidence**: Git worktrees omit git-ignored `local.properties`, causing `./gradlew` and `xcodebuild` pre-build script invocations (`:shared:embedAndSignAppleFrameworkForXcode` in [`iosApp/project.yml`](../iosApp/project.yml)) to fail with `SDK location not found` unless `ANDROID_HOME` is exported in the build command environment. Documenting the `ANDROID_HOME` fallback alongside `local.properties` notes would avoid repeated failures in fresh worktrees.
- **Suggested Layer**: `.agents/skills/`

### [OPEN] ISS-005: Pre-Existing Swift 6 Actor-Isolation & Localization Deprecation Warnings
- **Date / Context**: 2026-11-21 (Spec 76 — Local Favorites Persistence & Favorites Screen)
- **Category**: `code-inconsistency`
- **Symptom & Evidence**: Building `iosApp` and `macosApp` emits pre-existing compiler warnings in untouched Apple views: [`KFImage.swift`](../iosApp/iosApp/KFImage.swift#L193) (`Placeholder` `#IsolatedConformances` and main-actor static `downsample(data:maxPixelSize:)` call) and [`UserProfileView.swift`](../iosApp/iosApp/UserProfileView.swift#L385) (`appendInterpolation` deprecation warning). Addressing these will restore strict zero-warning Swift builds across all targets.
- **Suggested Layer**: `specs/` (or scoped Swift cleanup task)

---

## Dismissed Issues

*(None yet — intentional trade-offs or rejected proposals closed as `[DISMISSED]` with an explicit rationale are recorded here so future sessions do not re-flag them.)*

---

## Resolved Issues

*(None yet — resolved entries are moved here or archived with their resolution reference when triaged via `issue-log-resolution`.)*
