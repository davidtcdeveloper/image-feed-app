# Architectural Modularization Strategies for Native-UI Kotlin Multiplatform Projects Across Android, iOS, and macOS

## 1. Architectural Topologies for Shared Kotlin Multiplatform Logic

Designing the modular boundaries of a Kotlin Multiplatform codebase supporting Android, iOS, and macOS with native user interfaces requires balancing domain encapsulation against compiler pipeline overhead. In an architecture where presentation rendering is handled by platform-native frameworks—Jetpack Compose on Android, SwiftUI on iOS, and SwiftUI or AppKit on macOS—the shared Kotlin codebase encapsulates domain models, network communication, persistence, analytics, and presentation state machines. Choosing a modular topology governs team autonomy, compilation performance, Application Binary Interface (ABI) stability, and binary distribution across Gradle and Xcode build systems.

### Horizontal Layer-Based Modularization

Horizontal modularization segregates shared code across technical responsibilities throughout the application lifecycle. Under this topology, the project is divided into broad horizontal layers such as foundational models, network infrastructure, database persistence, domain repositories, and shared presentation logic. Foundational modules manage network engines, serialization pipelines, and database drivers, while upstream modules encapsulate domain interactors and reactive state holders.

While horizontal layering provides clear structural categorization and is straightforward for small teams to adopt, it exhibits severe operational bottlenecks as the codebase expands. Any modification to low-level shared logic, such as altering a domain entity to support an additional property, triggers a cascading recompilation across data repositories, domain use cases, and every downstream consumer. Furthermore, feature ownership becomes fragmented across technical boundaries; delivering a single business capability requires engineers to modify files across multiple technical layers simultaneously, increasing merge conflict frequency and degrading continuous integration throughput.

### Vertical Feature-Based Modularization

Vertical modularization organizes the codebase into independent, domain-driven feature modules such as authentication, payment processing, content feeds, and user settings. Each feature module encapsulates its own internal data sources, repository implementations, domain use cases, and platform-agnostic state containers. Cross-cutting infrastructure is delegated to a set of core modules that provide HTTP client engines, native database drivers, and common utilities.

Vertical isolation allows cross-functional product teams to operate autonomously within explicit module boundaries without invalidating the build caches of adjacent features. Feature ownership maps directly to organizational units, and incremental compilation times improve significantly because changes remain isolated to the modified domain. However, purely vertical topologies present integration friction when features must communicate. When one domain requires data managed by another, direct module dependencies risk introducing cyclic dependency graphs, which Gradle prohibits.

### The Hybrid API-versus-Implementation Pattern

The hybrid approach resolves inter-feature coupling by dividing each business domain into contract and implementation module pairs. The contract module exposes pure Kotlin interfaces, domain models, immutable state classes, and event definitions, while depending only on lightweight core abstractions. The implementation module contains concrete network services, SQL queries, repository logic, and internal state orchestration.

Downstream features depend exclusively on the contract module via standard Gradle `implementation` dependencies. Concrete implementations are bound at runtime or during compile-time dependency injection assembly at the application root. This topology yields substantial technical advantages:

- **Minimal ABI Invalidation:** Modifications to internal business logic or network serialization rules in an implementation module leave the public Application Binary Interface unchanged. Consequently, Gradle incremental compilation skips recompiling dependent consumer modules, maximizing local and remote cache hits.
- **Strict Encapsulation:** Encapsulation is strongly enforced by marking concrete repository classes and internal helpers with Kotlin's `internal` visibility modifier, preventing architecture erosion where consumers bypass domain boundaries.
- **Fast Test Execution:** Automated tests in dependent modules can link against lightweight interface mocks or fakes without compiling SQLite drivers or HTTP engines into test binaries.

#### Architectural Topologies Comparison

| Evaluation Dimension | Horizontal Layer-Based | Vertical Feature-Based | Hybrid API / Implementation Separation |
| :--- | :--- | :--- | :--- |
| **Team Autonomy** | Low (Frequent lockstep pull requests across layers) | High (Teams isolated by product domain) | Maximum (Teams work against published interface contracts) |
| **Build Invalidation Scope** | Broad (Base layer edits recompile the entire project) | Moderate (Scoped to the feature and direct consumers) | Minimal (Implementation edits avoid consumer recompilation) |
| **Encapsulation Enforcement** | Weak (Data sources visible across entire technical layer) | Moderate (Features risk leaking internal classes) | Strong (Internal visibility enforced via module boundary) |
| **Total Module Count** | Low (4–8 modules total) | Moderate (10–25 modules) | High (2N + Core modules, doubling feature count) |
| **Configuration Complexity** | Low | Moderate | High (Requires convention plugins and DI automation) |

---

## 2. Packaging and Apple Target Consumption: Umbrella Frameworks and Linking Dynamics

While Android consumes multi-module Kotlin Multiplatform codebases naturally via Gradle project dependencies and Android Archive (AAR) artifacts, the Apple ecosystem imposes fundamental binary linking constraints. When targeting iOS and macOS simultaneously with native UI layers, the packaging topology directly dictates binary footprint, Objective-C symbol visibility, and Xcode link latency.

### The Multi-Framework Anti-Pattern in Kotlin/Native

A frequent design error in multi-module KMP architectures is compiling each Gradle module into an independent native binary framework and linking all of them into the target Xcode project. This approach encounters structural limitations in the Kotlin/Native compiler toolchain:

- **Duplicated Runtime Overhead:** Kotlin/Native generates a dedicated runtime environment—including memory allocators, garbage collectors, and standard library primitives—inside every statically compiled framework. Compiling multiple independent frameworks duplicates this runtime overhead across binaries, inflating application bundle size.
- **Type Collisions and Incompatible Types:** If two separate frameworks consume a common domain type from a shared core module, the compiler exports the declaration independently into both binary headers. When consumed by Swift, these declarations are treated as distinct, incompatible types, triggering type mismatch compiler errors and symbol collisions during dynamic linker resolution.
- **Lack of Native Framework Dependency Chains:** Kotlin/Native does not support binary-level framework dependency chains where one compiled native framework links against another.

### The Umbrella Framework Architecture

To maintain shared type identity and eliminate redundant runtime footprints, multi-module architectures consolidate shared logic into an umbrella framework. An assembly module, typically named `:umbrella` or `:shared`, serves as the single compilation gateway for Apple platforms. This module links internal feature implementations via standard Gradle dependencies and selectively exposes public API contracts to Swift using the native export configuration.

Consolidating compilation within an umbrella module ensures that the compiler emits a single, unified Objective-C header file and module map, guaranteeing global type identity across all Swift callers.

```kotlin
// umbrella/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    val appleTargets = listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64(),
        macosX64()
    )

    appleTargets.forEach { target ->
        target.binaries.framework {
            baseName = "SharedKit"
            isStatic = true

            // Re-export contract APIs so Objective-C/Swift can see their public symbols
            export(projects.feature.auth.api)
            export(projects.feature.checkout.api)
            export(projects.core.model)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.auth.impl)
            implementation(projects.feature.checkout.impl)

            api(projects.feature.auth.api)
            api(projects.feature.checkout.api)
            api(projects.core.model)
        }
    }
}
```

### Build Cost Drivers: Header Generation versus Framework Topology

Empirical build benchmarking demonstrates the compilation overhead associated with different linking topologies. Testing three-module dependency cascades under Kotlin/Native link pipelines provides precise measurements of build duration and toolchain latency.

| Link Configuration / Scenario | Build Duration | Overhead vs. Swift Baseline | Primary Cost Driver |
| :--- | :--- | :--- | :--- |
| **Swift Baseline** (3-module cascade) | 5.71 s | 1.0× (Baseline) | Standard Clang/Swift AST generation |
| **KMP: 3 Separate Frameworks** | 16.34 s | 2.86× | Parallel link execution (`org.gradle.parallel=true`) |
| **KMP: Umbrella Exporting All 3 Modules** | 17.76 s | 3.11× | Monolithic Objective-C bridging header generation |
| **KMP: Umbrella Exporting Only Module A** | 8.38 s | 1.47× | Minimal header emission; internal types skip Obj-C bridging |

When Gradle parallel task execution is active, compiling three separate frameworks incurs a total duration driven by the slowest single link task (16.34 s). An umbrella framework exporting all three modules requires 17.76 s, driven by the synthesis of extensive Objective-C bridging headers and type stubs. When intermediate modules are compiled into the binary but excluded from the Gradle export statement, the umbrella build time drops by more than 50% to 8.38 s.

This demonstrates that build cost is primarily driven by the volume of Kotlin declarations exposed across the bridging boundary rather than the framework topology itself. Internal logic, private data models, and implementation details should remain unexported.

### Unified Multi-Platform Packaging and Apple Consumption

To target iOS devices, iOS simulators, and macOS architectures simultaneously, the umbrella framework must be packaged as an XCFramework. Standard Apple framework directories cannot combine slices intended for the same platform family or combine mobile iOS binaries with desktop macOS binaries. Utilizing the Kotlin Multiplatform XCFramework DSL aggregates all configured Apple slices into a single artifact, synthesizing tasks to generate debug and release bundles.

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

val xcf = XCFramework("SharedKit")

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64(),
        macosX64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "SharedKit"
            isStatic = true
            xcf.add(this)
        }
    }
}
```

Consuming this compiled artifact within Xcode projects is typically handled through three distinct integration workflows:

1. **Local Direct Swift Package Manager Integration:** In a monorepo configuration, a local Swift package references the generated XCFramework directory directly via a relative file path. Xcode consumes the package locally without requiring Git tag manipulation or external networking.
2. **Remote SPM Binary Distribution:** In multi-repo environments, continuous integration pipelines assemble and zip the XCFramework, compute a SHA-256 checksum, and publish the binary archive to an artifact repository. A dedicated Swift package repository hosts the manifest referencing this remote binary. This decouples Apple developers from local JDK and Gradle requirements, though it introduces publishing latency during cross-platform iterations.
3. **CocoaPods Integration:** The legacy CocoaPods plugin generates podspec manifests and manages target integration automatically. However, it introduces Ruby environment management overhead and slower dependency resolution, leading modern teams to transition toward native Swift Package Manager workflows.

---

## 3. Swift Interoperability Bridges: SKIE versus Native Swift Export

Consuming Kotlin declarations through traditional Objective-C bridging headers creates usability friction in Swift. Suspending functions lose structured cancellation, sealed class hierarchies lose exhaustive pattern matching, and reactive Flow streams lack native bindings.

The **SKIE** compiler plugin resolves this by post-processing generated Apple binaries to produce idiomatic Swift interfaces. It transforms Kotlin `StateFlow` and `SharedFlow` instances into native Swift `AsyncSequence` streams, and adapts sealed class hierarchies into exhaustive Swift enums with associated values. SKIE serves as a standard bridge for delivering ergonomic Kotlin APIs to native SwiftUI and AppKit view layers.

In parallel, **native Swift Export** bypasses Objective-C header generation entirely by emitting direct Swift bindings from Kotlin compiler representations. This native export mechanism produces idiomatic `async`/`await` and `AsyncSequence` signatures while reducing the bridging header compilation costs observed in earlier benchmarks.

### Evaluating Direct Swift Export: Architectural and Modularization Implications

Recent implementations of Kotlin's official Swift Export toolchain—specifically evaluated in real-world scenarios such as the PeopleInSpace migration—demonstrate how direct Swift generation transforms KMP project modularization and packaging. Swift Export bypasses Objective-C bridging entirely, using the compiler DSL (`export { swift { } }`) to emit first-class Swift bindings directly from Kotlin compiler representations. While this eliminates third-party post-processing tools like SKIE and the `multiplatform-swiftpackage` plugin, it introduces critical architectural requirements that heighten the importance of API-versus-implementation modularization:

#### The Dedicated Export Umbrella Module (`:ios-export`)

A primary architectural takeaway from empirical Swift Export testing is the necessity of establishing a dedicated, minimal umbrella module (such as `:ios-export`) rather than placing the export configuration directly inside the shared business modules.

The Swift Export compiler processes dependencies according to their Gradle dependency configuration:

- Any library or project module declared as an `api` dependency of the directly exported module is exported in full, causing every public declaration in that dependency to be synthesized into Swift code.
- If an export block is placed directly on a common feature module, all of its shared `api` dependencies (such as Koin or Kermit) are analyzed and exported, pulling in large transitive graphs such as `kotlinx-coroutines-core`. In real-world migrations, this generated over 17,000 lines of unneeded Swift bindings that failed compilation.
- By isolating the export definition inside an independent `:ios-export` assembly module that depends on `:common`, the `:common` module itself is exported, while its internal dependencies are exported only transitively—restricting generated Swift stubs strictly to types directly reachable from the exposed API surface.

#### Enforcing Strict Encapsulation and Secondary Extraction

Because Swift Export directly translates public Kotlin symbols into Swift bindings, any loose encapsulation causes immediate code bloat and compilation instability in Xcode. Adopting Swift Export mandates two modular disciplines:

1. **Explicit API Mode and Internal Visibility:** Projects must enable Kotlin's `explicitApi()` compiler mode to force explicit visibility modifiers. Repository implementations, network engines, internal DTOs, and dependency injection wiring should be explicitly marked `internal` so the Swift compiler does not synthesize bindings for them.
2. **Database Isolation (`:db` Module):** Automated code generators such as SQLDelight emit public queries and driver interfaces. To prevent these generated SQL classes from leaking into the Swift export surface, database operations should be factored out into a dedicated `:db` module and imported into feature modules strictly via `implementation` dependencies.

When pairing a dedicated export umbrella module with `explicitApi()` enforcement and isolated database modules, the generated Swift code footprint in production scenarios drops from ~18,000 lines to ~2,600 lines—a reduction of more than 85% in synthesized bridge volume.

#### Swift API Ergonomics and Native Mapping

When consumed from native SwiftUI and AppKit view layers, direct Swift Export introduces several ergonomic advantages over legacy Objective-C bridging:

- **Namespace Mapping:** Kotlin package structures map directly to Swift namespaces (e.g., calling `di.personListViewModel()` and importing `remote.Assignment` instead of interacting with flattened `KoinKt` Objective-C utility classes).
- **Pattern-Matching Sealed Classes:** Swift Export attaches a `.sealedType()` accessor to exported sealed classes, mapping them to native Swift enums that support exhaustive Swift `switch` control flow.
- **Typed Flows:** Shared reactive streams are exported as `KotlinTypedStateFlow<T>`, providing typed `.value` accessors and an `.asAsyncSequence()` bridging function that integrates natively into SwiftUI `.task` and asynchronous iteration blocks.
- **Interface Implementations:** Any Swift class implementing an exported Kotlin interface must explicitly inherit from `KotlinBase`.

#### Toolchain Adjustments, Constraints, and Failure Modes

Migrating from the traditional umbrella framework to direct Swift Export alters project configurations and presents several operational constraints:

- **Toolchain and Linker Flags:** The Xcode build phase changes from executing `:common:embedAndSignAppleFrameworkForXcode` to `./gradlew :ios-export:embedSwiftExportForXcode`. Furthermore, because removing plugins like SQLDelight from the exported target removes their automated linker injections, native flags such as `-lsqlite3` must be manually appended to the Xcode project's `OTHER_LDFLAGS`.
- **Minimum OS Deployment Target (iOS 18+):** Because Swift Export's underlying Kotlin coroutine integration relies on Swift's modern synchronization primitives (`Mutex`), apps adopting direct Swift Export must raise their deployment target to iOS 18 or macOS 15, preventing adoption in projects that must support iOS 16 or 17.
- **Silent Runtime Crashes with HIDDEN Visibility:** Libraries can be hidden from export using `SwiftExportVisibility.HIDDEN` to generate lightweight stubs. However, if an exported Kotlin function exposes a return type originating from a hidden library (e.g., an `initKoin()` function returning Koin's `KoinApplication`), the generated Swift stub compiles successfully but synthesizes a `fatalError()` call, triggering silent application crashes at runtime upon invocation. Public entry points must be sanitized to return primitive or standard types (such as `Unit`).

---

## 4. Dependency Injection and Inversion-of-Control Across Multi-Module Boundaries

A multi-module architecture that separates contracts from implementations requires an inversion-of-control mechanism to wire implementations into abstract contracts without introducing circular Gradle dependencies or runtime failures.

### Runtime Service Locator Mechanisms

Koin is widely utilized in Kotlin Multiplatform projects due to its pragmatic setup, lightweight DSL, and zero code-generation overhead. Each feature implementation module registers its dependencies within a local Koin module, and the central umbrella module includes these sub-graphs during initialization.

However, Koin validates dependencies strictly at runtime. Missing dependencies, circular resolutions, or configuration mismatches fail during application launch via runtime exceptions rather than failing the build. Furthermore, the central assembly file must manually import and register every feature module's Koin definition, creating a configuration bottleneck that generates merge conflicts across distributed teams.

### Compile-Time Dependency Injection

To enforce compile-time verification without relying on JVM reflection, multiplatform architectures utilize compile-time frameworks like `kotlin-inject`. Operating via Kotlin Symbol Processing (KSP), `kotlin-inject` validates graph completeness during the build. Missing bindings or cyclic references fail compilation directly, preventing runtime crashes in production.

However, pure `kotlin-inject` requires manual component composition, meaning components must explicitly inherit or reference parent scopes. This retains a degree of centralized wiring that can limit modular autonomy.

### Automated Contribution Aggregation

To eliminate centralized configuration files entirely, modular multiplatform codebases incorporate automated contribution aggregation patterns:

- **`kotlin-inject-anvil`:** Extends `kotlin-inject` by introducing contribution annotations that allow feature modules to bind implementations directly to shared scopes without explicit references in the central component. During compilation, the processor discovers all contributed bindings across the classpath and automatically merges them into the root graph.
- **Metro Framework:** Advances this model by operating as a native Kotlin compiler plugin within the FIR and IR pipelines rather than running as a secondary KSP processing pass. Metro generates direct constructor calls in bytecode without emitting intermediate source files on disk, avoiding KSP configuration complexity and improving build performance across large multi-module repositories.

#### Dependency Injection Framework Comparison

| Evaluation Dimension | Koin | kotlin-inject (Pure) | kotlin-inject-anvil | Metro |
| :--- | :--- | :--- | :--- | :--- |
| **Validation Mechanism** | Runtime lookup | Compile-time (KSP) | Compile-time (KSP) | Compile-time (Compiler Plugin) |
| **Module Autonomy** | Low (Manual module list aggregation) | Moderate (Explicit component inheritance) | High (Automated contribution merging) | High (Automated contribution merging) |
| **Compilation Overhead** | Negligible (No code-generation tasks) | Moderate (Per-module KSP processing) | High (Multi-target KSP metadata passes) | Low to Moderate (Direct IR generation) |
| **Target Support** | Android, iOS, macOS | Android, iOS, macOS | Android, iOS, macOS | Android, iOS, macOS |
| **Ecosystem Status** | Highly Mature | Stable | Maintenance mode | Production-Targeted |

---

## 5. Presentation Layer Boundaries and Native UI Interoperability

In architectures where Android, iOS, and macOS each maintain separate UI implementations, establishing a precise boundary between shared Kotlin code and native presentation logic is critical. The goal is to maximize state management reuse without compromising platform-specific user experience or lifecycle requirements.

### ViewModel Placement and State Modeling

Shared feature modules encapsulate presentation state modeling and intent processing using multiplatform lifecycle primitives. By leveraging `androidx.lifecycle:lifecycle-viewmodel`, ViewModels reside entirely within shared common Kotlin code.

```kotlin
// feature/auth/impl/src/commonMain/kotlin/LoginViewModel.kt
open class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun submitLogin(credentials: Credentials) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            _uiState.value = when (val result = loginUseCase(credentials)) {
                is Result.Success -> LoginUiState.Success(result.data)
                is Result.Failure -> LoginUiState.Error(result.error.message)
            }
        }
    }
}
```

Placing ViewModels in shared code ensures that asynchronous task management, input validation, state transitions, and error handling are implemented and verified once through cross-platform unit tests.

### Platform-Native UI Consumption

Native presentation layers observe shared state streams using idiomatic patterns tailored to each platform:

On **Android**, Jetpack Compose screens consume shared ViewModels directly through lifecycle-aware state collection, binding state flows into composable hierarchies:

```kotlin
// Android Jetpack Compose
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Render Compose UI based on state
}
```

On **iOS and macOS**, SwiftUI views require state holders to conform to the `ObservableObject` protocol or use the modern Swift Observation framework. Utilizing SKIE, observable bridges, or Swift Export's native `KotlinTypedStateFlow.asAsyncSequence()` allows SwiftUI views to consume Kotlin state streams through native property wrappers and asynchronous task loops:

```swift
// Native SwiftUI View (iOS and macOS)
import SwiftUI
import SharedKit

struct LoginView: View {
    @StateObject private var viewModel: LoginViewModelWrapper

    var body: some View {
        VStack {
            switch viewModel.state {
            case .idle:
                IdleFormView(onSubmit: viewModel.submit)
            case .loading:
                ProgressView("Authenticating...")
            case .success(let user):
                HomeScreen(user: user)
            case .error(let message):
                ErrorBanner(message: message)
            }
        }
        .task {
            await viewModel.observeState()
        }
    }
}
```

### Decoupling Navigation Across Operating Systems

While presentation state is cleanly shared, navigation paradigms diverge fundamentally across operating systems:
- **Android** relies on single-activity backstacks and Compose Navigation paths.
- **iOS** emphasizes hierarchical navigation stacks, modal presentations, and split views.
- **macOS** utilizes desktop-oriented interactions, including persistent window sidebars, menu bar commands, floating panels, and multi-window management.

Enforcing a unified, cross-platform navigation framework across native UI layers compromises platform-specific usability and ergonomics. The recommended pattern delegates navigation execution entirely to native platform code. The shared ViewModel emits one-off navigation events through channels or flows, which the native UI layers collect to execute platform-native routing mechanisms.

---

## 6. Build Toolchain Performance, Gradle Caching, and CI/CD Orchestration

Modularizing a Kotlin Multiplatform codebase increases the total number of Gradle tasks, compiler passes, and binary targets. Maintaining responsive local iteration times and cost-effective continuous integration requires systematic toolchain optimization.

### Gradle Optimization and Compilation Caching

Large-scale multi-module KMP codebases must configure core Gradle performance settings in `gradle.properties` to ensure maximum concurrency and caching efficiency:

```properties
# Enable parallel project execution
org.gradle.parallel=true

# Cache task outputs across clean builds and branch switches
org.gradle.caching=true

# Retain configuration phase state across builds
org.gradle.configuration-cache=true

# Kotlin K2 compiler incremental compilation optimizations
kotlin.incremental=true
kotlin.native.cacheKind=none
```

Disabling native caching via `kotlin.native.cacheKind=none` resolves binary cache incompatibilities that can arise between K2-compiled libraries and native linkers during framework compilation.

Furthermore, as Gradle advances **Project Isolation**, the build engine decouples the configuration phases of independent projects to enable parallel configuration and instant configuration caching. Engineering teams must ensure that custom convention plugins avoid cross-project model references, such as `rootProject.allprojects`, to maintain compatibility with isolated execution requirements.

### Xcode Build Integration Workflows

When building native iOS and macOS applications within Xcode during local development, acquiring the compiled Kotlin framework can be structured using two operational models:

- **Direct Script Phase Approach:** Configures Xcode to execute Gradle tasks during every build invocation. In conventional framework setups, this runs `embedAndSignAppleFrameworkForXcode`, whereas in Swift Export environments, it executes `embedSwiftExportForXcode`. While this guarantees that Kotlin modifications are immediately reflected in the running application, it forces Xcode to wait for Gradle daemon configuration, task graph verification, and header generation checks on every build, introducing noticeable development latency.
- **Decoupled Local Binary Workflow:** Separates Xcode builds from continuous Gradle execution. Engineers assemble the umbrella XCFramework on demand or through automated background file watchers, while Xcode links against the pre-compiled binary via a local Swift package. This allows Apple platform engineers to iterate on native SwiftUI views and macOS layouts with near-instant build times, running Gradle tasks only when modifying shared Kotlin contracts.

### Continuous Integration Pipeline Architecture

Continuous integration pipelines must account for runner infrastructure economics. macOS runner minutes are substantially more expensive than standard Linux runners. An optimized multiplatform CI pipeline partitions tasks across a specialized execution matrix, balancing speed and infrastructure costs.

#### CI/CD Pipeline Stage Matrix

| Pipeline Stage | Agent Operating System | Executed Gradle & Toolchain Tasks | Economic & Operational Rationale |
| :--- | :--- | :--- | :--- |
| **Validation & Analysis** | Linux (Ubuntu) | `./gradlew commonTest detekt ktlint` | Highly cost-effective; executes all platform-agnostic business logic tests rapidly |
| **Android Assembly** | Linux (Ubuntu) | `./gradlew assembleRelease` | Linux runners compile AAR and APK artifacts without requiring macOS virtualization |
| **Compile-Time DI Verification** | Linux (Ubuntu) | `./gradlew kspCommonMainKotlinMetadata` | Validates dependency injection integrity at compile time before launching native tasks |
| **Apple Native Verification** | macOS (Apple Silicon) | `./gradlew iosSimulatorArm64Test macosArm64Test` | Executes Apple-specific native tests within native OS runtime environments |
| **Framework Assembly** | macOS (Apple Silicon) | `./gradlew assembleSharedKitXCFramework` | Requires Xcode command-line tools to assemble and slice multi-target XCFrameworks |
| **Native Packaging** | macOS (Apple Silicon) | `xcodebuild -workspace ... archive` | Signs and packages iOS IPA and macOS application bundles for distribution |

---

## 7. Comprehensive Evaluation and Decision Framework

Selecting an appropriate modularization topology requires evaluating organizational scale, domain complexity, and operational trade-offs. A structured comparison across candidate architectures illustrates the relative trade-offs across critical operational dimensions:

#### Modularization Pattern Trade-offs Matrix

| Architectural Metric | Pattern A: Monolithic Shared Module | Pattern B: Horizontal Layered Modules | Pattern C: Vertical Feature Modules | Pattern D: Hybrid API / Impl Feature Modules |
| :--- | :--- | :--- | :--- | :--- |
| **Feature Isolation** | None (Single codebase boundary) | Technical only (Domain leakage across layers) | High (Isolated by business capability) | Total (Strict interface contracts) |
| **Parallel Development Velocity** | Low (High contention on shared files) | Moderate (Merge conflicts in data modules) | High (Teams operate within feature bounds) | Maximum (Zero conflict on internal implementation changes) |
| **Incremental Build Performance** | Poor (Any modification triggers full rebuild) | Poor (Core layer edits invalidate downstream layers) | Good (Recompilation limited to feature tree) | Best (Implementation changes do not invalidate consumer ABIs) |
| **Xcode Linking Overhead** | Minimal (Single framework compile) | Minimal (Single umbrella compile) | High if multiple; Low via umbrella | Minimal (Only API interfaces exported to Swift) |
| **Setup & Maintenance Drag** | Minimal | Low | Moderate | High (Requires convention plugins and DI tooling) |
| **Target Team Scale** | 1–4 Engineers | 3–8 Engineers | 8–25 Engineers | >25 Engineers (Multiple dedicated squads) |

### Strategic Decision Criteria

Architectural choices should align directly with team size, domain volatility, build pipeline capacity, and platform target constraints:

- **Enterprise Scale (>20 Developers):** Teams of more than twenty developers operating across multiple autonomous squads should implement the **Hybrid API/Implementation pattern**. When continuous integration queues are a primary bottleneck, eliminating ABI invalidation across feature boundaries provides measurable build performance returns that justify maintaining separate contract and implementation modules. Furthermore, strict interface isolation prevents architectural leakage in larger engineering organizations where multiple teams contribute to shared infrastructure.
- **Mid-Sized Teams (5–20 Developers):** Teams consisting of five to twenty engineers should implement **Vertical Feature Modularization** without separate API/implementation module pairs. This topology provides domain ownership, parallel code evolution, and module-level encapsulation without doubling the project's total Gradle module count. Inter-feature communication can be managed cleanly through shared core repositories or lightweight interface inversion within shared modules.
- **Small Teams / Early Stage (<5 Developers):** Small teams of fewer than five developers maintaining rapidly evolving prototypes should retain a **single shared module** or a simple horizontal layering structure. Introducing granular multi-module boundaries prematurely creates unnecessary build script maintenance overhead and slows domain refactoring during early product exploration.

When evaluating whether to adopt **Direct Swift Export** over the established SKIE / Objective-C Umbrella Framework pattern, teams must factor in operating system baselines and boundary enforcement discipline:

- **Deployment Target Requirement:** If the product must support iOS 16 or iOS 17, teams must remain on SKIE and XCFramework linking, as Swift Export requires iOS 18+ and macOS 15+ due to native concurrency mutex primitives.
- **Modular Encapsulation Readiness:** Swift Export heavily penalizes loose encapsulation by bloating synthesized Swift code and exposing silent runtime stubs. Teams adopting Swift Export must be prepared to extract third-party generators (e.g., database code into `:db`) and enforce `explicitApi()` mode alongside a dedicated `:ios-export` umbrella module.

### Conclusions and Strategic Blueprint

For an enterprise Kotlin Multiplatform application supporting Android, iOS, and macOS with platform-native user interfaces, the optimal architecture balances vertical domain isolation with a consolidated native binary pipeline:

1. **Domain Isolation:** The shared Kotlin repository is structured around vertical business domains, complemented by foundational infrastructure modules for networking, persistence, and core data models. Business features isolate private repository logic and network queries within implementation packages, exposing public state holders, models, and interfaces via contract boundaries.
2. **Apple Distribution Pipeline:** For Apple target distribution, all shared modules are consolidated under a single umbrella compilation gateway:
   - In production environments targeting iOS 16 or 17, this is packaged as an XCFramework supporting iOS device, iOS simulator, and macOS architectures, post-processed with SKIE for idiomatic Flow and enum consumption, with export statements restricted strictly to public API contracts.
   - In modern greenfield environments targeting iOS 18+ and macOS 15+, the architecture can transition to direct Swift Export via a dedicated `:ios-export` umbrella module. This eliminates SKIE dependencies and generates native Swift namespaces and `.sealedType()` enums, while isolating database and internal dependencies using `explicitApi()` and implementation scopes to keep the generated bridge lean and prevent runtime crash traps.
3. **Decoupled Xcode Workflow:** The native Swift layer consumes this compiled framework through a local Swift Package Manager binary manifest within the monorepo, decoupling Xcode UI development from mandatory Gradle rebuilds.
4. **Compile-Time Dependency Injection:** Dependency injection is managed via compile-time mechanisms using `kotlin-inject` paired with automated contribution merging. Feature modules register implementations directly to shared application scopes using contribution annotations, allowing the umbrella graph to merge bindings automatically without requiring manual wiring in root build configurations.
5. **Presentation Layer Boundary:** Presentation state holders reside in shared code using multiplatform ViewModels, ensuring that business rules, state machines, and asynchronous lifecycles are implemented and tested once. Navigation routing and UI rendering are delegated entirely to native platform toolchains—Jetpack Compose on Android, SwiftUI on iOS, and SwiftUI or AppKit on macOS—ensuring uncompromised user experience and native ergonomics across all target platforms.

---

## 8. Evaluation and Modularization Strategy for Image Feed App

### 8.1 Current Codebase State Assessment & Codebase Smells Inventory

The current codebase is organized as follows:
- **Project Structure**: A single monolithic shared module (`:shared`), an Android app (`:androidApp`), and native Apple applications (`iosApp`, `macosApp`) configured via XcodeGen ([`iosApp/project.yml`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/project.yml)) embedding `:shared.framework`.
- **Shared Architecture**:
  - Presentation is standardized on **Shared MVI / UDF** with [`MviViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt), [`UiState`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiState.kt), [`UiIntent`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiIntent.kt), and [`UiEffect`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiEffect.kt).
  - Business logic spans distinct domains: Feed/Random (`presentation/feed`, `presentation/random`), Photo Details (`presentation/details`), Unified Search (`presentation/search`), Collections (`presentation/collections`, `presentation/collection`), and User Profiles (`presentation/profile`).
  - Data access is unified under [`UnsplashRepository`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/repository/UnsplashRepository.kt) backed by [`UnsplashApiClient`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/api/UnsplashApiClient.kt) and Ktor.
  - Compile-time dependency injection is handled via **Metro** (`dev.zacsweers.metro`) in [`Metro.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt), exposing [`ApplicationGraph`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt) and [`MetroHelper`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt).
- **Native UI Shells**:
  - **Android**: Jetpack Compose, Material 3 design tokens, Navigation 3 scenes, and adaptive layouts (`androidx.compose.material3.adaptive`).
  - **iOS & macOS**: SwiftUI views adhering to `GlassTheme` tokens, consuming shared ViewModels via `@Observable` Swift wrappers and calling `viewModel.clear()` upon `deinit`.

#### Codebase Smells Inventory Identified During Modularization Audit

An in-depth structural inspection of `:shared` revealed six primary architectural smells that must be addressed during modularization:

1. **Monolithic Repository Anti-Pattern (`UnsplashRepository` / `UnsplashApiClient`)**:
   [`UnsplashRepository`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/repository/UnsplashRepository.kt) and [`UnsplashApiClient`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/api/UnsplashApiClient.kt) operate as massive god-abstractions spanning 17+ distinct methods across 6 unrelated domains (Editorial Feed, Topics, Random Photos, Search, Collections, User Profiles, Photo Details, and Download Tracking). Any modification to an endpoint signature in one domain forces recompilation across all features and invalidates Gradle build caches globally.
2. **"God Fake" in Unit Testing**:
   Every ViewModel unit test file (`FeedViewModelTest`, `PhotoDetailsViewModelTest`, `UnifiedSearchViewModelTest`, `CollectionsFeedViewModelTest`, `CollectionDetailViewModelTest`, `UserProfileViewModelTest`, `RandomPhotoViewModelTest`) implements a bespoke `FakeUnsplashRepository` stubbing all 17 interface methods—even though each test exercises only 1 to 3 methods. This creates extreme test fragility: modifying any repository signature breaks all seven test suites simultaneously.
3. **Cross-Cutting Download Tracking Duplication & Coupling**:
   Download tracking (`trackDownload(downloadLocationUrl: String)`) is duplicated across five ViewModels (`FeedViewModel`, `PhotoDetailsViewModel`, `RandomPhotoViewModel`, `CollectionDetailViewModel`, `UserProfileViewModel`) and coupled directly into `UnsplashRepository`. Download tracking is an Unsplash API compliance requirement that belongs in an isolated, reusable tracker utility rather than polluting domain repositories.
4. **Domain Model Smells & Misplaced Entities**:
   In [`Photo.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/model/Photo.kt), feature-specific models such as `PhotoStats` (plus `StatMetric`, `HistoricalStats`, `StatsValue`)—used exclusively by Photo Details—and `UserStats`—used exclusively by User Profile—are bundled alongside core entities. This violates domain boundaries and prevents features from independently evolving their data models.
5. **Horizontal Visibility Leaks & Lack of Encapsulation**:
   Because all repository and client code currently resides in a shared layer, all repository implementations and API client endpoints must be marked `public`. This prevents using Kotlin's `internal` visibility modifier to hide network details, leaking implementation specifics across feature boundaries and expanding the Objective-C bridging header surface in Apple native builds.
6. **Misplaced Algorithmic Utilities (`CollectionsExt`)**:
   [`CollectionsExt.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/util/CollectionsExt.kt) (`filterDistinctAgainst`) provides pagination stream deduplication across network loads, yet resides in an untyped root utility package. It belongs in foundational network and collection processing infrastructure.

---

### 8.2 Evaluation of Candidate Modularization Strategies: Option A vs. Option B

When modularizing `:shared`, two primary strategies emerge for structuring data access and feature ownership:
- **Option A (Horizontal Core Data Layer)**: Extract a centralized `:core:data` module containing `UnsplashRepository` and `UnsplashApiClient`. Feature modules (`:feature:*`) contain only MVI ViewModels and presentation logic, depending horizontally on `:core:data`.
- **Option B (Vertical Feature Ownership)**: Eliminate `:core:data`. Each vertical feature module (`:feature:*`) owns its complete vertical slice: presentation ViewModels, domain repository contracts, concrete repository implementations, dedicated API clients, feature-specific domain models (e.g., `PhotoStats` in `:feature:details`, `UserStats` in `:feature:profile`), and lightweight test fakes. Shared network engine primitives, `BuildKonfig`, `PhotoDownloadTracker`, and `CollectionsExt` reside in `:core:network`.

#### Detailed Comparison Matrix: Option A vs. Option B

| Dimension | Option A: Horizontal Core Data (`:core:data`) | Option B: Vertical Feature Ownership (Recommended) |
| :--- | :--- | :--- |
| **Domain Ownership** | **Fragmented**: Presentation lives in `:feature:*`, but all data logic is coupled inside a centralized `:core:data` layer. | **Complete**: Each feature owns its full vertical stack from MVI presentation down to API client and repository. |
| **Repository Scope** | **Monolithic**: Retains a shared `UnsplashRepository` god-interface or aggregates multiple repositories in one horizontal module. | **Focused**: Granular, feature-owned repositories (`PhotoDetailsRepository`, `FeedRepository`, `SearchRepository`, `CollectionsRepository`, `ProfileRepository`). |
| **API Client Design** | Single shared `UnsplashApiClient` with 17+ endpoints; any endpoint change triggers recompilation of all features. | Dedicated, feature-scoped API clients (`PhotoDetailsApiClient`, `FeedApiClient`, etc.) querying only required endpoints. |
| **Unit Test Fakes** | Fragile "God Fakes": tests must stub all shared repository methods or share a heavy test fixture library. | Lean Feature Fakes: `FakePhotoDetailsRepository` only implements 2 methods (`getPhotoDetails`, `getPhotoStats`), completely isolated. |
| **Domain Models** | `PhotoStats` and `UserStats` remain trapped in `:core:model` or `:core:data` to satisfy the shared repository. | Feature-specific models live inside their owning feature (`PhotoStats` in `:feature:details`, `UserStats` in `:feature:profile`). `:core:model` contains only truly universal entities. |
| **Encapsulation (`internal`)** | Weak: Repository implementations and API clients must be `public` so feature modules can consume them across Gradle boundaries. | Strong: Repository implementations and API clients are `internal` to feature modules, fully encapsulated and invisible to other features. |
| **Apple Linking Overhead** | Higher: Public repository and data types in `:core:data` risk being exposed or analyzed by Kotlin/Native Obj-C export pipelines. | Minimal: `internal` repository implementations and API clients are never synthesized into Objective-C bridging headers, reducing link times. |
| **Download Tracking** | Retained inside monolithic repository or duplicated across feature ViewModels. | Extracted into a dedicated `PhotoDownloadTracker` in `:core:network`, consumed cleanly by features. |
| **Migration Risk** | Lower initial refactor, but preserves architectural debt and god-interface smells. | Slightly more initial file restructuring, but completely eliminates technical debt and aligns with long-term modular architecture. |

**Verdict**: **Option B (Vertical Feature Ownership)** is strongly recommended. It eliminates the monolithic repository anti-pattern, resolves fragile god-fakes, enforces strict encapsulation via `internal` visibility, isolates build caches, and optimizes Apple framework linking.

---

### 8.3 Recommended Architecture Blueprint (Option B)

```mermaid
graph TD
    subgraph Platform UI Shells
        Android["androidApp (Compose + Nav 3)"]
        Apple["iosApp / macosApp (SwiftUI)"]
    end

    subgraph Assembly & Umbrella
        Umbrella[":shared (Umbrella Framework & Metro Graph)"]
    end

    subgraph Feature Modules [Vertical Feature Modules (Presentation + Repository + API Client)]
        FeatFeed[":feature:feed\n(FeedVM, FeedRepo, FeedApiClient)"]
        FeatDetails[":feature:details\n(PhotoDetailsVM, PhotoDetailsRepo, PhotoDetailsApiClient, PhotoStats)"]
        FeatSearch[":feature:search\n(SearchVM, SearchRepo, SearchApiClient)"]
        FeatCollections[":feature:collections\n(CollectionsVMs, CollectionsRepo, CollectionsApiClient)"]
        FeatProfile[":feature:profile\n(ProfileVM, ProfileRepo, ProfileApiClient, UserStats)"]
    end

    subgraph Core Modules [Foundational Core Modules]
        CoreNetwork[":core:network\n(Ktor Engine, BuildKonfig, PhotoDownloadTracker, CollectionsExt)"]
        CoreModel[":core:model\n(Universal Entities: Photo, Topic, Collection, User, SearchResponse)"]
        CoreMvi[":core:mvi\n(MviViewModel, UiState, UiIntent, UiEffect, CommonFlow)"]
    end

    Android --> Umbrella
    Apple --> Umbrella
    Umbrella --> FeatFeed
    Umbrella --> FeatDetails
    Umbrella --> FeatSearch
    Umbrella --> FeatCollections
    Umbrella --> FeatProfile

    FeatFeed --> CoreNetwork
    FeatFeed --> CoreModel
    FeatFeed --> CoreMvi

    FeatDetails --> CoreNetwork
    FeatDetails --> CoreModel
    FeatDetails --> CoreMvi

    FeatSearch --> CoreNetwork
    FeatSearch --> CoreModel
    FeatSearch --> CoreMvi

    FeatCollections --> CoreNetwork
    FeatCollections --> CoreModel
    FeatCollections --> CoreMvi

    FeatProfile --> CoreNetwork
    FeatProfile --> CoreModel
    FeatProfile --> CoreMvi

    CoreNetwork --> CoreModel
```

---

### 8.4 Proposed Module Breakdown (Option B)

#### 1. Core Infrastructure Modules
- **`:core:model`**:
  - **Contents**: Pure, universally shared Kotlin domain entities and serialization DTOs ([`Photo.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/model/Photo.kt), [`Topic.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/model/Topic.kt), [`PhotoCollection.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/model/PhotoCollection.kt), [`CollectionSummary.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/model/CollectionSummary.kt), `User`, `SearchResponse`).
  - **Exclusions**: Feature-scoped models (`PhotoStats`, `UserStats`) are explicitly excluded and placed inside their owning feature modules.
  - **Dependencies**: Minimal (Kotlin serialization JSON only). Highly cacheable, rarely invalidated.
- **`:core:mvi`**:
  - **Contents**: Presentation foundation primitives ([`MviViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/MviViewModel.kt), [`UiState.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiState.kt), [`UiIntent.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiIntent.kt), [`UiEffect.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/mvi/UiEffect.kt), [`CommonFlow.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/util/CommonFlow.kt)).
  - **Dependencies**: Coroutines core, `androidx.lifecycle.viewmodel`.
- **`:core:network`**:
  - **Contents**: Ktor client engine factory (`createUnsplashHttpClient`), logging, content negotiation, `BuildKonfig` (Unsplash API key extraction), dedicated `PhotoDownloadTracker` (handling compliant download tracking calls), and algorithmic deduplication utility [`CollectionsExt.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/util/CollectionsExt.kt) (`filterDistinctAgainst`).
  - **Dependencies**: Ktor client bundles (OkHttp, Darwin, ContentNegotiation, Logging), `:core:model`.
- **Note on `:core:data`**: Under Option B, a centralized `:core:data` module is **not created**. Data access is vertically owned by feature modules.

#### 2. Vertical Feature Modules
Each feature module encapsulates its entire vertical slice:
- **`:feature:details`**:
  - **Presentation**: [`PhotoDetailsViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/feature/details/src/commonMain/kotlin/com/example/imagefeed/presentation/details/PhotoDetailsViewModel.kt), `PhotoDetailsState`, `PhotoDetailsIntent`, `PhotoDetailsEffect`.
  - **Data / Domain**: `PhotoDetailsRepository` contract, `internal class PhotoDetailsRepositoryImpl`, `internal class PhotoDetailsApiClient`.
  - **Feature Models**: `PhotoStats`, `StatMetric`, `HistoricalStats`, `StatsValue`.
  - **Testing**: `FakePhotoDetailsRepository` in `commonTest`.
  - **Dependencies**: `:core:model`, `:core:mvi`, `:core:network`.
- **`:feature:feed`**:
  - **Presentation**: [`FeedViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/feed/FeedViewModel.kt), `FeedState`, `FeedIntent`, `FeedEffect`, plus [`RandomPhotoViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModel.kt).
  - **Data / Domain**: `FeedRepository` contract, `internal class FeedRepositoryImpl`, `internal class FeedApiClient`.
  - **Testing**: `FakeFeedRepository` in `commonTest`.
  - **Dependencies**: `:core:model`, `:core:mvi`, `:core:network`.
- **`:feature:search`**:
  - **Presentation**: [`UnifiedSearchViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/search/UnifiedSearchViewModel.kt), `SearchState`, `UnifiedSearchIntent`, `UnifiedSearchEffect`.
  - **Data / Domain**: `SearchRepository` contract, `internal class SearchRepositoryImpl`, `internal class SearchApiClient`.
  - **Testing**: `FakeSearchRepository` in `commonTest`.
  - **Dependencies**: `:core:model`, `:core:mvi`, `:core:network`.
- **`:feature:collections`**:
  - **Presentation**: [`CollectionsFeedViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/collections/CollectionsFeedViewModel.kt) and [`CollectionDetailViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/collection/CollectionDetailViewModel.kt).
  - **Data / Domain**: `CollectionsRepository` contract, `internal class CollectionsRepositoryImpl`, `internal class CollectionsApiClient`.
  - **Testing**: `FakeCollectionsRepository` in `commonTest`.
  - **Dependencies**: `:core:model`, `:core:mvi`, `:core:network`.
- **`:feature:profile`**:
  - **Presentation**: [`UserProfileViewModel`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/profile/UserProfileViewModel.kt), `UserProfileState`, `UserProfileIntent`, `UserProfileEffect`.
  - **Data / Domain**: `ProfileRepository` contract, `internal class ProfileRepositoryImpl`, `internal class ProfileApiClient`.
  - **Feature Models**: `UserStats`.
  - **Testing**: `FakeProfileRepository` in `commonTest`.
  - **Dependencies**: `:core:model`, `:core:mvi`, `:core:network`.

#### 3. Assembly and Umbrella Gateway (`:shared`)
- **Maintains the Apple Linking Contract**: Compiles the single static framework `shared.framework` via `embedAndSignAppleFrameworkForXcode` consumed by [`iosApp/project.yml`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/project.yml).
- **Dependency Injection Aggregator**: Configures the root Metro `@DependencyGraph` ([`ApplicationGraph`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt)) and [`MetroHelper`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt), assembling feature repositories and ViewModels.
- **Selective Binary Export**: Re-exports public feature ViewModels and core contracts to Swift while keeping internal feature repositories, API clients, and network engines unexported, minimizing Objective-C bridging header overhead.
- **Transitional Role**: Temporarily holds legacy `UnsplashRepository` during Phase 1 & 2 until remaining features migrate in Phase 3, at which point legacy data classes are completely retired.

---

### 8.5 Phased Implementation Roadmap (Option B)

1. **Phase 1: Foundation Extraction (`:core:model`, `:core:mvi`, `:core:network`)**
   - Extract foundational types: `:core:model` (universal entities), `:core:mvi` (MVI primitives), `:core:network` (Ktor client engine, `BuildKonfig`, `PhotoDownloadTracker`, and `CollectionsExt`).
   - `:core:data` is **not** extracted in Phase 1; legacy `UnsplashRepository` temporarily remains in `:shared`.
   - Verify build stability across Android and iOS/macOS.
2. **Phase 2: Pilot Feature Migration (`:feature:details`)**
   - Migrate Photo Details to an isolated vertical feature module containing `PhotoDetailsViewModel`, `PhotoDetailsRepository`, `PhotoDetailsApiClient`, `PhotoStats`, and `FakePhotoDetailsRepository`.
   - Validate multi-module Metro DI assisted injection (`PhotoDetailsViewModel.Factory`) and Xcode umbrella framework linking.
3. **Phase 3: Remaining Feature Extractions (`:feature:feed`, `:feature:search`, `:feature:collections`, `:feature:profile`)**
   - Extract `feed`, `search`, `collections`, and `profile` into vertical feature modules owning their repositories, API clients, fakes, and models (`UserStats`).
   - Completely retire and delete legacy `UnsplashRepository` and `UnsplashApiClient` from `:shared`.
   - Fix all test paths and verify isolated feature test suites.
4. **Phase 4: Umbrella Framework & Linking Optimization**
   - Configure selective binary export in `:shared` to export only public feature ViewModels and core models.
   - Leverage `internal` visibility of feature repositories and clients to eliminate unnecessary Objective-C symbol generation and maximize link speed.
   - Configure Gradle build caching (`org.gradle.caching=true`, `org.gradle.parallel=true`) and CI test runner partitioning.


