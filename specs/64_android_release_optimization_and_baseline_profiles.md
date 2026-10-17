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

1. **R8 for release**: enable `isMinifyEnabled` and `isShrinkResources` for `release`; create `androidApp/proguard-rules.pro` with only the rules proven necessary (prefer library consumer rules; add app rules for serialized `NavKey`/DTO classes if needed). Verify the release APK runs all flows (Feed, Search, Details with Maps, Profile, Collections, Random photo, shake toast) - serialization and reflection issues appear only at runtime.
2. **Macrobenchmark + baseline profile module**: add a `:benchmark` (or `:baselineprofile`) Android test module targeting `androidApp`, with a `benchmark` build type derived from release (debug-signed, non-debuggable). Journeys: cold start to Feed, fling the Feed grid, open Details, scroll chart, open Profile, switch tabs. Record Baseline Profile generation and `StartupTimingMetric`/`FrameTimingMetric` benchmarks.
3. **ProfileInstaller**: add `androidx.profileinstaller` to `androidApp` so profiles apply on non-Play installs and on first launch; commit the generated `baseline-prof.txt` under `androidApp/src/main/` (generated artifact checked in, per Google guidance).
4. **Version catalog**: add the new artifacts/plugins to `gradle/libs.versions.toml` (no hard-coded versions in module scripts), and document the generation command in the `verification-and-linting` skill by reference (CLI one-liner only; no multi-line snippets).
5. **Baseline numbers**: capture before/after startup and jank metrics on a physical mid-range device or Gradle Managed Device and record them in this spec (or `specs/researches/`) so Specs 60-63 can reference them.
6. Out of scope: Play Store upload signing, CI automation (note as follow-up), iOS.

## 3. Verification

- `./gradlew :androidApp:assembleRelease` succeeds with zero R8 warnings requiring suppression; smoke test every screen on the release build.
- Macrobenchmark runs on a device/GMD and produces startup and frame timing results for `CompilationMode.None` vs `Partial` with the baseline profile.
- Confirm profile is packaged (`baseline-prof.txt`/`.dm` in the release artifact) and `profileinstaller` initializes.
- Dual-platform policy: shared code is untouched; still run the Android debug build, and the iOS build if any `shared/` change slips in (`AGENTS.md` invariant 2).
- Security invariant: no API keys or `local.properties` content committed in generated files.

## 4. Acceptance Criteria

- [ ] `proguard-rules.pro` exists; release build has R8 minification and resource shrinking enabled.
- [ ] Release APK smoke-tested on all main flows with no runtime crashes from shrinking.
- [ ] Macrobenchmark module added with startup and scroll/jank benchmarks; versions managed in the version catalog and verified compatible with AGP 9.x.
- [ ] Baseline profile generated, committed, and `androidx.profileinstaller` included.
- [ ] Before/after startup and frame-timing numbers recorded.
- [ ] Zero compiler warnings; no secrets committed; Android debug and release builds pass.

> [!NOTE]
> Line numbers in this spec are approximate (working tree had uncommitted edits at authoring time); locate code by symbol name.
