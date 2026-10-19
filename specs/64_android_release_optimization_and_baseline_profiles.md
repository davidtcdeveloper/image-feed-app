# Specification 64: Android Release Optimization, Baseline Profiles & Macrobenchmark

**Status:** Approved

## 1. Overview & Problem Statement

Compose performance in release builds depends on R8 and on AOT-compiled Baseline Profiles (jank on first run is dominated by JIT/class loading; `android/` and `graphics-performance.md` references). This app currently ships none of that. This group is build-infrastructure only and is independent from Specs 60-63, but it provides the measurement harness those specs use for before/after numbers, so it should land first or in parallel.

### Verified findings

- [`androidApp/build.gradle.kts`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/build.gradle.kts) `release` build type: `isMinifyEnabled = false` (no R8 shrinking/optimization, no resource shrinking configured).
- It references `proguard-rules.pro`, but **that file does not exist** (`find` for `proguard*` in the repo returned 0 results; `androidApp/` contains only `build/`, `build.gradle.kts`, `src/`). Enabling minification will not work until it is created, and keep rules are unknown.
- No `androidx.profileinstaller`, `androidx.benchmark` (macrobenchmark) or baseline-profile Gradle plugin in `gradle/libs.versions.toml`, root `build.gradle.kts`, or `settings.gradle.kts` (grep found none). `androidApp/src` has only `main` and `test`.
- Toolchain context: AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, Coil 3.6.3, Metro 1.4.5, minSdk 29, compileSdk/targetSdk 37; the module uses the new `ApplicationExtension` DSL. Compatibility of the Baseline Profile Gradle plugin and macrobenchmark with AGP 9.x must be verified by the implementer (check current androidx.benchmark / baselineprofile plugin release notes) before choosing versions.
- Libraries needing consumer/keep consideration under R8: kotlinx.serialization (shared DTOs), Ktor/OkHttp via `:shared`, Coil 3, Maps Compose / Play Services Maps, Metro-generated graph (compile-time, expected to need few rules), Navigation 3 `NavKey` serialization, Compose.

## 2. Technical Design

The implementation is structured into two distinct phases to decouple immediate build-time R8 shrinking from device-dependent baseline profile generation:

### Phase A: R8 Minification, Resource Shrinking & Proguard Keep Rules
1. **R8 for release**: enable `isMinifyEnabled = true` and `isShrinkResources = true` in `androidApp/build.gradle.kts` `release` build type.
2. **Create `androidApp/proguard-rules.pro`**: configure necessary keep rules while preferring consumer rules where provided:
   - **Kotlinx Serialization**: keep `@Serializable` class members and companion serializers (`-keepclassmembers class * { @kotlinx.serialization.Serializable <fields>; }`, `-keepclassmembers class *$Companion { ... }`).
   - **Navigation 3 `NavKey`**: keep implementations of `androidx.navigation3.runtime.NavKey` and `AppRoute` sealed hierarchy.
   - **Metro Compile-Time Graphs**: keep Metro dependency graphs and generated provider accessors if stripped by aggressive R8 shrinking.
   - **Play Services Maps & Ktor/OkHttp**: preserve Maps API models and OkHttp engine reflection points.
3. **Release Smoke Testing**: verify `./gradlew :androidApp:assembleRelease` builds cleanly with zero R8 warnings requiring ungrounded suppressions, and smoke test all flows (Feed, Search, Details with Maps, Profile, Collections, Random photo, shake toast).

### Phase B: Macrobenchmark & Baseline Profile Infrastructure
1. **Macrobenchmark + baseline profile module**: add a `:benchmark` (or `:baselineprofile`) Android test module targeting `androidApp`, with a `benchmark` build type derived from release (debug-signed, non-debuggable).
   - Verify compatibility of `androidx.benchmark:benchmark-macro-junit4` and `androidx.baselineprofile` plugin with AGP 9.4.1 (or use a direct `com.android.test` module configuration if plugin version resolution conflicts with AGP 9.x).
2. **ProfileInstaller & Profile Generation**: add `androidx.profileinstaller` to `androidApp`; run baseline profile generator on an active connected physical device, emulator, or Gradle Managed Device (GMD) to produce `baseline-prof.txt` under `androidApp/src/main/`.
3. **Version catalog**: add all benchmark/baseline artifacts to `gradle/libs.versions.toml` (no hard-coded versions).
4. **Baseline numbers**: capture before/after startup and frame-timing metrics with `CompilationMode.None` vs `Partial` and record in this spec.

## 3. Verification

- `./gradlew :androidApp:assembleRelease` succeeds with zero R8 warnings requiring suppression; smoke test every screen on the release build.
- Macrobenchmark runs on a connected device/emulator/GMD and produces startup and frame timing results for `CompilationMode.None` vs `Partial` with the baseline profile.
- Confirm profile is packaged (`baseline-prof.txt`/`.dm` in the release artifact) and `profileinstaller` initializes.
- Dual-platform policy: shared code is untouched; still run the Android debug build, and the iOS build if any `shared/` change slips in (`AGENTS.md` invariant 2).
- Security invariant: no API keys or `local.properties` content committed in generated files.

## 4. Acceptance Criteria

### Phase A (R8 Minification & Hygiene)
- [ ] `androidApp/proguard-rules.pro` created with rules for Kotlinx Serialization DTOs, Navigation 3 `NavKey`, Metro graph, and Maps.
- [ ] `androidApp/build.gradle.kts` release build has `isMinifyEnabled = true` and `isShrinkResources = true`.
- [ ] Release APK compiles via `./gradlew :androidApp:assembleRelease` and smoke-tested on all main flows with no runtime crashes from shrinking.

### Phase B (Profiles & Benchmarking)
- [ ] Macrobenchmark module added with startup and scroll/jank benchmarks; versions managed in the version catalog and verified compatible with AGP 9.4.1.
- [ ] Baseline profile generated on device/emulator, committed under `androidApp/src/main/baseline-prof.txt`, and `androidx.profileinstaller` included.
- [ ] Before/after startup and frame-timing numbers recorded.
- [ ] Zero compiler warnings; no secrets committed; Android debug and release builds pass.

> [!NOTE]
> Line numbers in this spec are approximate (working tree had uncommitted edits at authoring time); locate code by symbol name.
