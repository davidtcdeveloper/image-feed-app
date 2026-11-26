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

### [OPEN] ISS-006: Upstream KSP `2.3.6` `nonSelfDeps` Blocks Global Gradle Isolated Projects
- **Date / Context**: 2026-11-23 (Spec 78 — `build-logic` Convention Plugins & Isolated Projects Readiness)
- **Category**: `tooling-friction`
- **Symptom & Evidence**: Running `./gradlew :androidApp:assembleDebug -Dorg.gradle.unsafe.isolated-projects=true` fails during `:core:database:kspAndroidMain` task creation (`Project ':core:database' cannot access task dependencies directly` in `KSP 2.3.6` `KspSubpluginKt.nonSelfDeps`). All 12 non-KSP modules pass strict Isolated Projects with 0 violations, so `org.gradle.unsafe.isolated-projects=true` remains unset in [`gradle.properties`](../gradle.properties) until KSP is upgraded past `2.3.6` and `:androidApp:assembleDebug -Dorg.gradle.unsafe.isolated-projects=true` passes with 0 problems.
- **Suggested Layer**: `wiki/` | `specs/`

### [OPEN] ISS-007: `.swiftformat` and `.swiftlint.yml` Conflict on Opening Braces and Trailing Commas
- **Date / Context**: 2026-11-25 (Spec 83 — Apple DesignSystem Modularization & Light-Mode Hygiene)
- **Category**: `tooling-friction`
- **Symptom & Evidence**: [`.swiftformat`](../.swiftformat) (`--commas always`, `--wrapparameters before-first`) formats multiline initializers with `{` on a new line and keeps trailing commas in array literals, triggering SwiftLint `opening_brace` and `trailing_comma` warnings under [`.swiftlint.yml`](../.swiftlint.yml) during `swiftlint lint iosApp/iosApp iosApp/iosAppTests iosApp/iosAppUITests`.
- **Suggested Layer**: `.agents/skills/` | `specs/`

### [OPEN] ISS-008: Wave-2 Spec Ownership Lists Drifted After Spec 83 Extraction (`for: .tabBar` & `⌘F`)
- **Date / Context**: 2026-11-26 (Spec 87 — Apple Float Navigation Shell & macOS Search)
- **Category**: `wiki-drift`
- **Symptom & Evidence**: [Spec 87](../specs/archive/87_apple_float_navigation_shell_and_macos_search.md) and [Spec 88](../specs/88_apple_satin_headers_zoom_transitions_and_unified_routes.md) were drafted before Spec 83 extracted `PhotosFeedView.swift`, so Spec 87's Disjoint Ownership note prohibited touching `PhotosFeedView.swift`, `CollectionsFeedView.swift`, `FavoritesView.swift`, and `SearchView.swift` even though Spec 83 had copied unconditional `.toolbar(.hidden, for: .tabBar)` into all four files (hiding the iOS 26+ `TabView` bar) and `.keyboardShortcut("f", modifiers: .command)` into `PhotosFeedView.swift`. Resolved for Spec 87 via `adaptiveTabBarVisibility()` and `openRootSearch` / `searchFocusTrigger`; check [`specs/88_apple_satin_headers_zoom_transitions_and_unified_routes.md`](../specs/88_apple_satin_headers_zoom_transitions_and_unified_routes.md) references (`ContentView.swift` lines vs extracted `PhotosFeedView.swift` / `SearchTabView`) during Spec 88 implementation.
- **Suggested Layer**: `specs/`

### [OPEN] ISS-009: `ui-visual-review` Apple Snapshot Commands Omit `TEST_RUNNER_SNAPSHOT_OUTPUT_DIR` on macOS & Ambiguous Simulator Name
- **Date / Context**: 2026-11-26 (Spec 89 — Apple Photo Details Editorial Parity & Glass Morphing)
- **Category**: `skill-gap`
- **Symptom & Evidence**: In [`.agents/skills/ui-visual-review/SKILL.md`](../.agents/skills/ui-visual-review/SKILL.md#L47-L55), running `SNAPSHOT_OUTPUT_DIR=... xcodebuild test -scheme macosApp -only-testing:macosAppTests/UIVisualSnapshotTests` without `TEST_RUNNER_SNAPSHOT_OUTPUT_DIR` drops the env var inside `macosApp` test runner (`UIVisualSnapshotTests.swift` falls back to temporary directory and deletes PNGs on teardown), and `-destination 'platform=iOS Simulator,name=iPhone 18 Pro'` becomes ambiguous when multiple iOS runtimes (for example iOS 27.0 and 27.2) have an `iPhone 18 Pro` simulator unless an OS version or device UDID (`id=...`) is specified.
- **Suggested Layer**: `.agents/skills/`

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
