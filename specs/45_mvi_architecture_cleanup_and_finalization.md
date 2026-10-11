# Specification 45: MVI Architecture Cleanup & Finalization

**Status:** Approved

## 1. Overview & Objectives

With all presentation features fully migrated to **Shared MVI / UDF** across [Spec 42](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/42_mvi_foundation_and_transition_guidelines.md), [Spec 43](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/43_mvi_pilot_photo_details_migration.md), and [Spec 44](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/44_mvi_core_screens_migration.md), this specification executes the final decommissioning of legacy Presenter infrastructure, eliminates the transition state from agent instructions, and formalizes Shared MVI as the sole, non-negotiable architectural standard.

---

## 2. Scope of Cleanup & Decommissioning

### A. Delete Legacy Presenter Files
Permanently remove obsolete files in accordance with the Zero Dead Code Policy:
1. [`shared/src/commonMain/kotlin/com/example/imagefeed/presentation/PresenterScope.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/PresenterScope.kt)
2. [`shared/src/commonMain/kotlin/com/example/imagefeed/presentation/PresenterFactories.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/PresenterFactories.kt)
3. [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/TestPresenterScopeFactory.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/TestPresenterScopeFactory.kt)
4. [`androidApp/src/main/java/com/example/imagefeed/android/util/PresenterViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/PresenterViewModel.kt)
5. Delete all old `*Presenter.kt` implementations in `shared/src/commonMain/kotlin/com/example/imagefeed/presentation/`.

### B. Clean Up Dependency Injection Graph ([`Metro.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt))
- Remove `DispatcherProvider`, `PresenterScopeFactory`, and all legacy `provide*PresenterFactory` bindings from `AppModule`.
- Remove legacy presenter factory properties from `ApplicationGraph`.
- Remove legacy `get*Presenter()` helper methods from `MetroHelper`.

### C. Finalize Agent Guidelines & Skill Documentation
1. **Update [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md)**:
   - Remove all mentions of the transition state and the Presenter pattern.
   - Establish **Shared MVI / UDF** as Core Invariant 1.
2. **Update [`.agents/skills/kmp-architecture/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/kmp-architecture/SKILL.md)**:
   - Rewrite Section 2 ("Shared Presenter & State Flow Pattern") into "Shared MVI & Unidirectional Data Flow Pattern".
   - Document `MviViewModel`, `UiState`, `UiIntent`, and `UiEffect` standards.
   - Document coroutine lifecycle management via `viewModelScope`.
   - Purge all references to `PresenterScope`, `presenter.clear()`, and `PresenterViewModel`.
3. **Update [`specs/steps.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/steps.md)**:
   - Update Step inventory and record completion of the MVI architecture migration.

---

## 3. Implementation Steps

- [ ] **Step 1: Delete Obsolete Presenter Classes & Infrastructure**
- [ ] **Step 2: Clean Up Metro DI Graph**
- [ ] **Step 3: Update `AGENTS.md` and `kmp-architecture/SKILL.md` to Final Architecture Standards**
- [ ] **Step 4: Update Documentation Inventory in `specs/steps.md`**
- [ ] **Step 5: Code Quality & Zero-Warning Audit**
  - Run `./gradlew ktlintFormat` and `./gradlew ktlintCheck detekt`.
  - Run `swiftformat .` and `swiftlint lint iosApp/iosApp`.
  - Run all shared tests: `./gradlew :shared:testDebugUnitTest`.
  - Run full dual-platform compilation:
    - Android: `./gradlew :androidApp:assembleDebug`
    - iOS: `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`

---

## 4. Acceptance Criteria

- [ ] Zero legacy presenter files, scope factories, or wrapper proxies remain in the repository.
- [ ] Zero dead code, unused imports, or deprecated warnings remain.
- [ ] [`AGENTS.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/AGENTS.md) and [`.agents/skills/kmp-architecture/SKILL.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/.agents/skills/kmp-architecture/SKILL.md) describe MVI as the sole architectural standard.
- [ ] Dual-platform builds and automated tests pass with 100% success.
