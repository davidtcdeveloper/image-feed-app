# Specification 81: Shared KMP Color Engine, Preview Seeding & Parallel Photo Details

**Status:** Accepted

* **Wave:** Wave 1 — Disjoint Foundations
* **Depends On:** None
* **Parallelizable With:** [Spec 82](82_android_design_system_matte_and_adaptive_panes.md), [Spec 83](83_apple_design_system_matte_and_light_mode_hygiene.md)
* **Research Mapping:** [Cross-Platform Material Craft Evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) — `ARCH-01`, `ARCH-02`, `ARCH-03` (shared KMP contract), Craft `2.7`
* **Exclusive File Ownership:** `core/model/**`, `feature/details/**`, `feature/feed/**`, [`Metro.kt`](../shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt)

---

## 1. Overview & Codebase Baseline

Auditing the repository post-Spec 80 against [the UI craft evaluation](researches/material_design_cross_platform_ui_craft_evaluation.md) reveals three shared KMP gaps that block downstream Android and Apple craft specs:

1. **Frame-0 Hero & Color-Scheme Flash (`ARCH-01`)**: [`PhotoDetailsViewModel.kt`](../feature/details/src/commonMain/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModel.kt#L24-L89) initializes `PhotoDetailsState()` with `photo = null` and then awaits Room (`favoritesRepository.getFavoriteById(photoId)`), `repository.getPhotoDetails(photoId)`, and `repository.getPhotoStats(photoId)` **sequentially**. On Android, Frame 0 mounts a centered `ExpressiveLoadingIndicator` rather than `PhotoCanvasPane`, breaking `Modifier.sharedElement` resolution and flashing default-to-photo color schemes. On Apple, `SearchView`, `CollectionDetailView`, and `UserProfileView` omit `initialPhoto`.
2. **Android-Locked Tonal Swatch Math (`ARCH-02`)**: [`Theme.kt`](../androidApp/src/main/java/com/example/imagefeed/android/theme/Theme.kt#L18-L225) houses pure-Kotlin HSL/Hex parsing (`parseHexArgb`, `deriveTonalSwatches`, `rgbToHsl`, `hslToColor`, `colorToHex`), leaving Apple without tonal color swatches or photo-hue accent derivation.
3. **`RandomPhotoViewModel.init` Auto-Fetch Contract (`ARCH-03`)**: [`RandomPhotoViewModel.kt`](../feature/feed/src/commonMain/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModel.kt#L21-L23) unconditionally dispatches `RandomPhotoIntent.FetchRandomPhoto()` inside `init {}`. [`FeedViewModel.swift`](../iosApp/iosApp/FeedViewModel.swift#L93-L112) and [`RandomPhotoViewModelTest.kt`](../feature/feed/src/commonTest/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModelTest.kt#L65-L93) rely on that auto-fetch, whereas Android `MainActivity` needs an idle-capable instance until a shake or randomize tap occurs.

---

## 2. Scope & Technical Workstreams

### Workstream A (`ARCH-02`): Shared Tonal Swatch & Accent Engine in `:core:model`
1. Extract pure-Kotlin color math into `core/model/src/commonMain/kotlin/com/example/imagefeed/model/color/PhotoColorEngine.kt` (automatically exported to Swift via `api(project(":core:model"))` in [`shared/build.gradle.kts`](../shared/build.gradle.kts#L16-L29)).
2. Expose `HexRgb` (`r`, `g`, `b`, `argb`), `PhotoTonalSwatch` (`hex`, `rgb`), `parseHexRgb(seedHex: String?): HexRgb?`, `deriveTonalSwatches(seedHex: String?): List<PhotoTonalSwatch>`, and `deriveAccentHex(seedHex: String?, isDark: Boolean): String?` (preserving 3/6/8-digit hex parsing, low-alpha rejection `alpha < 16`, achromatic neutral guard `saturation < 0.05f`, and 5 luminance steps `0.35f`, `0.50f`, `0.65f`, `0.75f`, `0.85f`).
3. Add deterministic `commonTest` coverage in `core/model/src/commonTest/kotlin/com/example/imagefeed/model/color/PhotoColorEngineTest.kt` covering valid 3/6/8-digit hexes, invalid/low-alpha inputs, neutral gray preservation, and light/dark accent derivation.

### Workstream B (`ARCH-01`): Ephemeral `NavigationPreviewCache` & Concurrent Details Fetch
1. Add `@SingleIn(AppScope::class)` in-memory LRU `NavigationPreviewCache` (`maxEntries = 16`, storing metadata DTOs `Photo`, `PhotoCollection`, `User` only — zero image binary or disk persistence per ADR-02 and [Business Rules](../wiki/business-rules.md#L10)) in `:core:model` (or `:core:mvi` / `Metro.kt` binding), plus `MetroHelper` seed/read accessors (`seedPhotoPreview`, `getPhotoPreview`, `seedCollectionPreview`, `seedUserPreview`) callable from both Android and Swift.
2. Add an optional `previewCache: NavigationPreviewCache? = null` constructor parameter to [`PhotoDetailsViewModel.kt`](../feature/details/src/commonMain/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModel.kt#L19-L28) (keeping existing constructor call sites in [`PhotoDetailsViewModelTest.kt`](../feature/details/src/commonTest/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModelTest.kt) source-compatible) and seed initial `PhotoDetailsState(photo = previewCache?.getPhoto(photoId))` synchronously on Frame 0 before Room fallback.
3. Refactor `PhotoDetailsViewModel.loadDetails()` ([`PhotoDetailsViewModel.kt`](../feature/details/src/commonMain/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModel.kt#L80-L89)) to execute `repository.getPhotoDetails(photoId)` and `repository.getPhotoStats(photoId)` concurrently via `coroutineScope { async ... }`, emitting `photo = details` as soon as `getPhotoDetails` completes without blocking behind `stats`, while surfacing errors cleanly if either request fails (rethrow `CancellationException`).
4. Add `commonTest` unit tests in `NavigationPreviewCacheTest` and [`PhotoDetailsViewModelTest.kt`](../feature/details/src/commonTest/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModelTest.kt) verifying Frame-0 cache hit, LRU eviction at 16 entries, and concurrent non-blocking `details` emission before delayed `stats`.

### Workstream C (`ARCH-03` KMP Contract): Idle-Capable `RandomPhotoViewModel`
1. Add `autoFetchOnInit: Boolean = true` to [`RandomPhotoViewModel.kt`](../feature/feed/src/commonMain/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModel.kt#L15-L24) with `@AssistedInject` / factory support in [`Metro.kt`](../shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt#L68-L90).
2. Keep no-arg `MetroHelper.getRandomPhotoViewModel()` defaulting `autoFetchOnInit = true` so [`FeedViewModel.swift`](../iosApp/iosApp/FeedViewModel.swift#L93) and [`RandomPhotoViewModelTest.kt`](../feature/feed/src/commonTest/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModelTest.kt) remain 100% compatible, while exposing `MetroHelper.getRandomPhotoViewModel(autoFetchOnInit: Boolean)` for Android `MainActivity` (`Spec 84`).
3. Annotate `MetroHelper.getFeedRepository()` ([`Metro.kt`](../shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt#L100-L102)) as `@Deprecated` pending deletion in Spec 84, and verify `autoFetchOnInit = false` behavior in [`RandomPhotoViewModelTest.kt`](../feature/feed/src/commonTest/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModelTest.kt).

---

## 3. Constraints, Non-Goals, Wiki & Skill Impact

* **Unsplash Compliance (`ADR-02`)**: `NavigationPreviewCache` holds strictly bounded (`<= 16`), ephemeral in-memory Kotlin metadata DTOs (`Photo`, `PhotoCollection`, `User`); zero image bytes or persistent files are stored.
* **Swift Interop Safety**: Any Kotlin declaration exposed solely for Swift (`MetroHelper` helpers, `PhotoColorEngine` helpers) must compile cleanly in ObjC/Swift static framework exports without name collisions (`@Suppress("unused") // Invoked on Swift code` where needed).
* **Non-Goals**: Do not modify UI files in `androidApp/` or `iosApp/iosApp/` (owned by Specs 82–89), and do not delete `MetroHelper.getFeedRepository()` until Spec 84 migrates `MainActivity.kt`.
* **Wiki Impact**:
  * [`wiki/architecture-and-modules.md`](../wiki/architecture-and-modules.md) — Document `PhotoColorEngine`, `NavigationPreviewCache`, and parameterized `RandomPhotoViewModel` factory.
  * [`wiki/performance-and-memory.md`](../wiki/performance-and-memory.md) — Document Frame-0 preview seeding (`maxEntries = 16`) and parallel `PhotoDetailsViewModel` loading.
* **Skill & Guideline Impact**:
  * [`.agents/skills/kmp-architecture/SKILL.md`](../.agents/skills/kmp-architecture/SKILL.md) — Update §3.B (`ViewModel Injection Patterns`) to move `RandomPhotoViewModel` to parameterized/assisted factory injection (`autoFetchOnInit`), and document `@SingleIn(AppScope::class)` `NavigationPreviewCache` (`maxEntries = 16`) + `:core:model` pure UI-color sharing (`PhotoColorEngine`).

---

## 4. Verification & UI Visual Review Plan

Per [`.agents/skills/verification-and-linting/SKILL.md`](../.agents/skills/verification-and-linting/SKILL.md) (shared `commonMain` changes require shared tests, Android build, iOS build, and Kotlin linters):
* **Unit & Integration Tests**: `./gradlew allTests`
* **Android & Apple Framework Compilation**:
  ```bash
  ./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinMacosArm64
  ```
* **iOS & macOS Downstream Compatibility Build**:
  ```bash
  xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme macosApp -destination 'platform=macOS,arch=arm64' build CODE_SIGNING_ALLOWED=NO
  ```
* **Formatting & Static Analysis**: `./gradlew ktlintCheck detekt`
* **UI Visual Review**: Not required (`commonMain` / `:shared` changes only; zero UI edits).

---

## 5. Acceptance Criteria

- [ ] `PhotoColorEngine` in `:core:model` implements `HexRgb`, `PhotoTonalSwatch`, `parseHexRgb`, `deriveTonalSwatches`, and `deriveAccentHex` with `commonTest` coverage.
- [ ] `NavigationPreviewCache` (`maxEntries = 16`, `@SingleIn(AppScope::class)`) caches metadata DTOs only and seeds `PhotoDetailsViewModel` synchronously on Frame 0 before Room fallback.
- [ ] `PhotoDetailsViewModel.loadDetails()` fetches details and stats concurrently, emitting `photo` immediately upon `getPhotoDetails` completion, verified by `PhotoDetailsViewModelTest`.
- [ ] `RandomPhotoViewModel` supports `autoFetchOnInit: Boolean = true` without breaking `FeedViewModel.swift` or existing tests, and `MetroHelper.getRandomPhotoViewModel(autoFetchOnInit: Boolean)` is exposed.
- [ ] `./gradlew allTests :androidApp:assembleDebug ktlintCheck detekt` and `xcodebuild` (`iosApp` + `macosApp`) pass with zero compiler warnings.
- [ ] Affected [`wiki/`](../wiki/README.md) pages and [`.agents/skills/kmp-architecture/SKILL.md`](../.agents/skills/kmp-architecture/SKILL.md) are updated.

---

## 6. Verification Record

### Verified
*(Pending implementation)*

### Unverified / Gaps
*(Pending implementation)*
