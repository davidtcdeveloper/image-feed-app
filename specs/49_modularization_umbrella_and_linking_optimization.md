# Specification 49: Modularization Phase 4 - Umbrella Framework & Linking Optimization

**Status:** Approved

## 1. Overview & Problem Statement

Following the completion of core module extractions ([Spec 46](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/46_modularization_foundation_core_extraction.md)), pilot feature migration ([Spec 47](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/47_modularization_pilot_feature_details.md)), and remaining feature extractions ([Spec 48](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/48_modularization_remaining_features_extraction.md)), **Phase 4** optimizes the multi-module build toolchain, Kotlin/Native linking pipeline, and local/CI performance under **Option B (Vertical Feature Ownership)**.

As demonstrated in Section 2 and Section 6 of [`specs/researches/modularization_evaluation.md`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/researches/modularization_evaluation.md#L99-L113), multi-module KMP architectures can encounter build latency if:
1. **Unconstrained Objective-C Bridging**: Exporting intermediate modules or internal networking DTOs into the umbrella Apple framework forces the Kotlin/Native compiler to synthesize massive Objective-C stubs, inflating Xcode link times from 8.38 s to over 17.76 s (a >2x penalty).
2. **Sub-optimal Gradle Concurrency & Caching**: Multi-module builds without fine-tuned parallel execution and configuration caching suffer from serialized task graphs and cache misses.
3. **Redundant CI Runner Costs**: Running all test and analysis tasks on expensive macOS runners drains CI budgets when platform-agnostic business logic can execute on fast Linux runners.

Under Option B, vertical feature ownership provides an intrinsic linking advantage: because each feature module's concrete repository implementation (`*RepositoryImpl`), dedicated API client (`*ApiClient`), and internal query parameters are marked with Kotlin's `internal` visibility modifier, the Kotlin/Native compiler automatically skips them during Objective-C header synthesis. Phase 4 hardens the umbrella compilation gateway, refines selective exports, and optimizes Gradle and Xcode build configurations.

---

## 2. Technical Architecture & Optimization Strategies

### A. Selective Binary Export in Umbrella (`:shared`) & Internal Visibility Benefits
To minimize the generated Objective-C bridging header footprint and maximize Swift compilation speed:
- **Exported Symbols (Public Surface)**:
  - Feature ViewModels and MVI contracts: `:feature:feed`, `:feature:details`, `:feature:search`, `:feature:collections`, `:feature:profile`.
  - Core domain models: `:core:model`.
  - Core MVI types: `:core:mvi` (`MviViewModel`, `UiState`, `UiIntent`, `UiEffect`, `CommonFlow`).
- **Unexported Symbols (Internal Implementation)**:
  - `:core:network` (Ktor client engine, JSON serializers, HTTP plugins, BuildKonfig, PhotoDownloadTracker) is linked statically into the binary but strictly unexported from Objective-C headers.
  - Feature-internal repositories and API clients (`internal class *RepositoryImpl`, `internal class *ApiClient`) are hidden within their respective feature modules and generate zero Objective-C bridging stubs.

```kotlin
// shared/build.gradle.kts
kotlin {
    val appleTargets = listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64()
    )

    appleTargets.forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true

            // Re-export only public presentation and model contracts
            export(projects.feature.feed)
            export(projects.feature.details)
            export(projects.feature.search)
            export(projects.feature.collections)
            export(projects.feature.profile)
            export(projects.core.model)
            export(projects.core.mvi)
            // projects.core.network and internal feature data implementations are intentionally NOT exported
        }
    }
}
```

### B. Toolchain Concurrency & Gradle Build Caching
Verify and configure performance flags in `gradle.properties`:
```properties
# Concurrency & daemon optimizations
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Kotlin K2 incremental compilation & Native link settings
kotlin.incremental=true
kotlin.native.cacheKind=none
```

### C. Xcode Build Integration & Linking Efficiency
- Verify that XcodeGen pre-build script in [`iosApp/project.yml`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/project.yml) continues to invoke `embedAndSignAppleFrameworkForXcode` targeting `:shared`.
- Verify the generated Xcode header contains only clean, namespaced ViewModels and models without leaking Ktor, internal serialization machinery, or internal repository details.
- Ensure `swiftformat` and `swiftlint` continue to run cleanly against the modernized Swift wrappers.

### D. Continuous Integration Execution Matrix
Align CI workflow execution with the optimized matrix from research section 6.3:
- **Linux (Ubuntu) Runners**:
  - Run all unit tests across all 8 modular libraries:
    `./gradlew :core:model:allTests :core:mvi:allTests :core:network:allTests :feature:feed:allTests :feature:details:allTests :feature:search:allTests :feature:collections:allTests :feature:profile:allTests`
  - Run static analysis: `./gradlew ktlintCheck detekt`
  - Assemble Android binary: `./gradlew :androidApp:assembleDebug`
- **macOS (Apple Silicon) Runners**:
  - Run native tests: `./gradlew iosSimulatorArm64Test macosArm64Test`
  - Verify Xcode build: `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`
  - Run Swift linters: `swiftformat --lint . && swiftlint lint iosApp/iosApp`

---

## 3. Implementation Steps

- [ ] **Step 1: Configure Selective Framework Exports in `:shared`**
  - Restrict `export(...)` declarations in `shared/build.gradle.kts` strictly to public feature modules and core model/MVI contracts.
  - Exclude `:core:network` and internal feature repositories/clients from binary export.
- [ ] **Step 2: Optimize `gradle.properties` Performance Settings**
  - Verify `org.gradle.parallel=true`, `org.gradle.caching=true`, and `org.gradle.configuration-cache=true`.
  - Validate that multi-module builds successfully hit the Gradle build cache on repeated invocations.
- [ ] **Step 3: Measure & Validate Linking Overhead**
  - Verify compilation time of `:shared:compileKotlinIosSimulatorArm64` and Xcode build.
  - Inspect generated Objective-C bridging header stubs to ensure no internal Ktor, DTO, or internal repository types leak into the Swift interface.
- [ ] **Step 4: Audit Dual-Platform Build & Verification**
  - Verify Android assembly: `./gradlew :androidApp:assembleDebug`.
  - Verify Apple build: `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`.
  - Run all tests across the repository: `./gradlew check`.
  - Run all formatters and linters (`ktlintFormat`, `ktlintCheck`, `detekt`, `swiftformat .`, `swiftlint`).

---

## 4. Acceptance Criteria

- [ ] `:shared` exports only public feature and domain contracts to the Apple framework.
- [ ] Internal feature repositories, feature API clients, and network engine classes are hidden via `internal` visibility and generate zero Objective-C bridging stubs.
- [ ] Centralized `:core:data` is completely absent from the module graph and build configuration.
- [ ] Gradle build caching and parallel execution operate cleanly without configuration cache failures.
- [ ] Android, iOS, and macOS applications build cleanly with zero warnings or linking errors.
- [ ] All tests pass across the entire multi-module graph.
