# Specification: Agent Guidelines & Rule Modernization

**Status:** Implemented

## 1. Overview & Purpose
Following recent major cross-platform developments—including compile-time Metro DI migration (`dev.zacsweers.metro`), Jetpack Navigation 3 adoption on Android, Material 3 design tokenization & UX modernization (SearchBar, PullToRefresh, Haptics), iOS 17+ Glassmorphism (`GlassTheme`, detented sheets, sensory feedback), and Xcode build configuration hardening—the developer and agent rules in `AGENTS.md` and `ai-rules/` have accumulated stale references, outdated build commands, and architectural omissions.

This specification tracks the systematic modernization of repository guidelines, agent instructions, and build tooling synchronization to reflect the codebase's current state and best practices.

---

## 2. Identified Inconsistencies & Target Updates

### A. Build Tooling & Command Standards
1. **Eliminate Prohibited `cd` Commands**:
   * Replace `cd iosApp && xcodegen && xcodebuild...` with non-`cd` commands across `AGENTS.md` and `ai-rules/build-and-deps.md`:
     ```sh
     xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
     ```
2. **Synchronize `iosApp/project.yml` with `project.pbxproj`**:
   * Commit `75b310c` added a shell guard in `project.pbxproj` to skip Gradle framework compilation when `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED="YES"`.
   * Add this shell guard to both `iosApp` and `macosApp` pre-build scripts in `iosApp/project.yml` so that regenerating via `xcodegen` preserves build settings.
3. **Acknowledge Active SwiftFormat Configuration**:
   * Update references in `AGENTS.md` from "(if SwiftFormat is configured)" to reflect that `.swiftformat` is configured and active.

### B. Dependency Injection & Metro Migration
1. **Purge Obsolete Koin References**:
   * Remove "Koin" from `ai-rules/build-and-deps.md` line 12 and document **Metro** (`dev.zacsweers.metro` 1.4.2).
2. **Document Metro Architecture in `AGENTS.md` & `architecture.md`**:
   * Specify compile-time dependency injection using Metro (`@DependencyGraph`, `@BindingContainer`, `@Provides`, `@SingleIn(AppScope::class)`, and `MetroHelper.graph`).

### C. Android Navigation 3 Architecture
1. **Document Jetpack Navigation 3**:
   * Add explicit guidelines in `AGENTS.md`, `architecture.md`, and `material-design.md` detailing the use of `androidx.navigation3` (`NavBackStack`, `NavDisplay`, `NavKey`, `NavEntry`).
   * Prohibit legacy `NavController` and Navigation 2 XML / Compose graph setups.
2. **Document Motion & Predictive Back**:
   * Document `SharedTransitionLayout` hero animations and Android predictive back gesture support (`android:enableOnBackInvokedCallback="true"`).

### D. Apple & Cross-Platform Design Foundations
1. **Document macOS Platform Target**:
   * Explicitly include `macosApp` alongside `androidApp` and `iosApp` as a declarative UI shell observing shared KMP state.
2. **Modernize `apple-design.md`**:
   * Document `.presentationBackgroundInteraction(.enabled(upThrough: .fraction(0.70)))` for non-modal interactive inspector sheets permitting simultaneous full-canvas zoom behind the sheet.
   * Document high-contrast dark scrim layering (`Color(hex: "0A0A0C").opacity(0.82)` over `.regularMaterial`) for WCAG AAA contrast compliance in Light Mode.
3. **Cross-Reference Design Rules in `AGENTS.md`**:
   * Summarize zero-hardcoded-color tokens and reference `ai-rules/design-principles.md`, `ai-rules/material-design.md`, and `ai-rules/apple-design.md`.

### E. Rule Loading & Inventory References
1. **Update `ai-rules/rule-loading.md`**:
   * Add routing for Dependency Injection (Metro in `shared/commonMain/di`) and Navigation 3 updates.
2. **Update Spec Index**:
   * Expand the reference spec directory in `AGENTS.md` and `specs/steps.md` to reference the complete set of specifications.

---

## 3. Execution Checklist
- [x] Create `specs/32_agent_rules_modernization.md` (this document).
- [x] Update `specs/steps.md` to add Step 34.
- [x] Update `iosApp/project.yml` with `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED` script guard.
- [x] Update `AGENTS.md` (commands, Metro DI, Navigation 3, macOS target, design summary, specs index).
- [x] Update `ai-rules/build-and-deps.md` (remove Koin, update Apple build command, note Metro).
- [x] Update `ai-rules/architecture.md` (Metro DI, Navigation 3, macOS shell, presenter disposal).
- [x] Update `ai-rules/rule-loading.md` (DI & Navigation routing).
- [x] Update `ai-rules/material-design.md` (Navigation 3, Shared Transitions, predictive back).
- [x] Update `ai-rules/apple-design.md` (interactive sheet detents & background interaction, contrast scrim).
- [x] Run formatting and linting tools to verify integrity.
- [x] Finalize `specs/32_agent_rules_modernization.md` to `Implemented` and `specs/steps.md` to `(Completed)`.
