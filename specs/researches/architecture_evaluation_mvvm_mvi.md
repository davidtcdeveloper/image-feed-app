# Architectural Blueprint for Heterogeneous Kotlin Multiplatform Applications

## Comparative Architectural Analysis: MVVM versus MVI in Heterogeneous KMP

Kotlin Multiplatform (KMP) shared cores driving platform-divergent user interfaces—specifically Jetpack Compose on Android and a unified SwiftUI implementation shared across iOS and macOS—introduce distinct constraints for state orchestration, event routing, and memory retention. Selecting between Model-View-ViewModel (MVVM) and Model-View-Intent (MVI) dictates how predictably state moves across the Kotlin/Native boundary into Darwin runtimes while maintaining native responsiveness.

### Architectural Mechanics and Qualitative Evaluation

The shared MVVM pattern relies on state holders exposing observable streams, conventionally structured around Kotlin Coroutines `StateFlow` and `SharedFlow`. The expansion of `androidx.lifecycle.ViewModel` into an official multiplatform artifact allows shared ViewModels to reside directly within `commonMain`, granting out-of-the-box access to `viewModelScope` across Android, iOS, and macOS. In Jetpack Compose, state collection integrates naturally through `collectAsStateWithLifecycle()`, which couples emission consumption with Android lifecycle states.

The primary advantage of MVVM in a multiplatform setting is low barrier to entry and rapid implementation velocity. Engineers leverage familiar Android patterns in shared code without extensive domain boilerplate, mutating state via direct function calls. However, MVVM introduces substantial architectural vulnerabilities when scaling across Darwin platforms.

Because `androidx.lifecycle.ViewModel` was conceived within Android's lifecycle paradigm, Darwin targets lack a native `ViewModelStoreOwner`. Without manual retention bridges or custom `IosViewModelStoreOwner` coordinators, SwiftUI views—which are value types recreated frequently during rendering cycles—risk either prematurely disposing of running coroutines or leaking `viewModelScope` allocations when views disengage from navigation hierarchies. Furthermore, when multiple independent `StateFlow` properties are exposed from a single ViewModel, concurrent asynchronous mutations can produce transient state desynchronization, commonly known as *torn reads*, wherein the UI observes mutually inconsistent field values during re-composition or re-rendering.

Model-View-Intent (MVI), alternatively formalized as Unidirectional Data Flow (UDF), addresses this state divergence by enforcing an immutable mathematical relation:

$$State_{t+1} = f(State_t, Intent)$$

Frameworks such as MVIKotlin and FlowMVI encapsulate state mutations within a singular, immutable data container, routing all user interactions through typed intent pipelines processed by an executor and reduced into state updates.

The fundamental benefit of MVI across heterogeneous UI platforms is the total elimination of ambiguous presentation states. Because the interface observes a single consolidated state stream, Compose and SwiftUI render identical functional states given identical sequences of historical intents. This determinism provides superior testability; domain logic and state transitions can be evaluated headlessly in pure multiplatform unit tests without instantiating UI hosts.

The trade-off resides in structural overhead and bridge performance. MVI requires strict separation between actions, intents, internal messages, and side-effect labels, which increases initial codebase verbosity. Moreover, emitting a unified, coarse-grained state object across the Kotlin/Native boundary upon every micro-interaction requires disciplined Swift-side observation. If the bridged state object is large and lacks granular property tracking, SwiftUI may execute broad view invalidations, reducing rendering efficiency on macOS and iOS unless coupled with modern Swift Observation primitives.

### Architectural Paradigm Comparison

| Architectural Attribute | Shared MVVM (`androidx.lifecycle.ViewModel`) | Shared MVI / UDF (MVIKotlin / Store) |
| :--- | :--- | :--- |
| **State Encapsulation** | Granular or consolidated `StateFlow` instances. | Strictly monolithic, immutable data class per component. |
| **Interaction Surface** | Direct, public ViewModel method invocations. | Sealed interface Intents dispatched to a single sink. |
| **Side-Effect Orchestration** | Ephemeral channels, `SharedFlow`, or fire-and-forget coroutines. | Explicit Label or Effect stream decoupled from view state. |
| **Jetpack Compose Interop** | Native via `collectAsStateWithLifecycle()`. | Native via `store.states.collectAsState()` or Decompose extensions. |
| **SwiftUI Interop** | Requires Swift state holders, native Swift Export `AsyncSequence`, or `KMP-ObservableViewModel`. | Requires intent dispatch wrappers and state sequence observation or `AsyncSequence` loops. |
| **Darwin Lifecycle Parity** | Asymmetric; requires synthetic retainers or deinit cleanups on iOS and macOS. | Symmetrical when anchored to lifecycle-aware component contexts. |
| **Concurrency Invariants** | Vulnerable to race conditions across multiple flow streams. | Thread-safe through sequential message handling and pure reducers. |
| **Codebase Overhead** | Minimal initial ceremony; higher long-term divergence risk. | Higher upfront structural ceremony; strict long-term maintainability. |

---

## Concurrency, Reactivity, and the Darwin Interoperability Layer

Sharing a single business and presentation core between Android and Darwin targets requires bridging Kotlin Coroutines and Kotlin/Native runtimes into Swift's structured concurrency model. While Jetpack Compose consumes Kotlin flows directly, the Darwin interop boundary introduces memory, threading, and lifecycle complications that require deliberate architectural mitigation.

### Modern Darwin Interoperability: Direct Swift Export and Linker Innovations

Historically, bridging Kotlin Multiplatform to Apple targets required routing through an intermediate Objective-C binary framework, which flattened generic types, obscured concurrency semantics, and boxed nullable primitives into Objective-C wrapper objects such as `KotlinInt` or `KotlinBoolean`. Contemporary Kotlin Multiplatform architectures leverage major updates to this layer:

#### First-Party Swift Export

Swift Export shifts Kotlin/Native compilation from generating legacy Objective-C headers (`.h`) to emitting native Swift modules directly:

- **Native Build Pipeline**: In Xcode build phases, legacy invocation of `embedAndSignAppleFrameworkForXcode` is replaced by `embedSwiftExportForXcode`, generating first-class Swift module declarations directly consumable by Swift files.
- **Preserved Packaging and Namespaces**: Each Kotlin Gradle module exports as an isolated Swift module, and Kotlin packages are preserved or flattened into Swift enums, eliminating mangled global symbol namespaces.
- **Direct Primitive Nullability**: Primitives like `Int?` or `Boolean?` translate directly into native optional Swift primitives (`Swift.Int32?`, `Swift.Bool?`) rather than boxed reference pointers.
- **Enums and Sealed Hierarchies**: Kotlin `enum class` instances export as regular native Swift enums conforming to `CaseIterable` and `RawRepresentable`. Sealed classes and interfaces generate a `.sealedType()` accessor that maps instances directly to Swift enum cases, enabling exhaustive `switch` pattern matching in SwiftUI without fallback `default:` statements.
- **Native Concurrency and AsyncSequence**: Suspend functions export natively as Swift `async throws`, while Kotlin `Flow` and `StateFlow` export directly as Swift `AsyncSequence` types.
- **Cross-Language Inheritance and Reverse Import**: Swift code can directly inherit from exported open Kotlin classes or implement Kotlin interfaces without intermediate Objective-C protocols. This enables native Apple framework implementations (such as CryptoKit, StoreKit 2, or macOS AppKit menu managers) to be injected directly back into shared Kotlin domain use cases.

#### Direct Swift Package Manager (SPM) Integration

The Kotlin Toolchain incorporates direct Swift Package Manager dependency support for Apple platform targets. Rather than managing complex CocoaPods configurations or manual binary frameworks, multiplatform Gradle builds can directly declare external Swift packages. Shared code targeting `appleMain`, `iosMain`, or `macosMain` can resolve and link against pure SwiftPM libraries seamlessly.

#### Tooling and Compiler Linkers (Touchlab SKIE vs. Swift Export)

Touchlab SKIE has historically served as the primary compiler plugin providing Swift-friendly concurrency, exhaustive enums, and `SkieSwiftAsyncSequence` bridging. While first-party Swift Export establishes the long-term native standard for Kotlin-to-Swift compilation, SKIE remains an active tool for projects using earlier compiler releases or specialized runtime optimizations. Architecting shared presentation contracts using standard `StateFlow` containers and sealed interfaces ensures full compatibility across both SKIE and native Swift Export.

#### IDE Workflow Separation

Reflecting modern KMP workflows, Swift language IDE editing features within Android Studio and IntelliJ IDEA have been discontinued in favor of dedicated tool separation: shared Kotlin modules, business logic, and Android Compose code are authored in Android Studio / IntelliJ, while Apple UI development (SwiftUI) and Swift integration are managed natively in Xcode.

### Concurrency and Runtime Edge Cases on Darwin

A critical vulnerability in cross-platform concurrency is the mismatch in cancellation semantics. When invoking Kotlin suspend functions from Swift, cancellation must propagate correctly down to the Kotlin `Job` hierarchy; otherwise, terminating a SwiftUI view or cancelling an enclosing Swift `Task` can leave background coroutines running indefinitely. Both native Swift Export and SKIE resolve this by tying Swift structured task lifecycles directly to Kotlin `Job.cancel()` triggers.

Main-thread dispatching represents another structural hurdle. While Android guarantees that `Dispatchers.Main.immediate` executes on the main Looper, Darwin platforms rely on the Grand Central Dispatch (GCD) Main Queue. In mixed multiplatform environments involving JVM desktop or complex Darwin background worker pools, coroutines driving UI state changes must explicitly synchronize with the main dispatch queue before emitting values, avoiding fatal threading violations in SwiftUI.

Exception handling introduces severe operational risk. In Kotlin, all exceptions are unchecked, whereas Swift enforces compile-time error declaration via `throws`. If a Kotlin suspend function or reactive stream throws an unhandled exception that crosses the Kotlin/Native boundary without an explicit `@Throws` annotation, the Darwin runtime treats the exception as fatal and terminates the process immediately. Robust cross-platform architectures resolve this by wrapping domain interactions within functional outcome types, such as `Result<T>`, or ensuring that all interop boundary methods strictly declare `@Throws`.

---

## Cross-Platform Adaptive Layout Architecture across Diverse Form Factors

The target application must scale across varied physical environments: compact mobile form factors, foldable devices exhibiting dynamically altering hinge postures, large tablet screens, and desktop displays on macOS characterized by continuous, arbitrary window resizing. Supporting these form factors requires decoupling semantic application state from platform-specific presentation mechanics.

### Android Adaptive Implementations

Jetpack Compose handles layout adaptation through the Material 3 Adaptive library suite (`androidx.compose.material3.adaptive`). Rather than querying raw screen dimensions, the application evaluates `WindowSizeClass` classifications (Compact, Medium, Expanded) and interrogates physical hinge states via `Posture` and `HingeInfo`.

The core architectural components for multi-pane layouts on Android are `NavigableListDetailPaneScaffold` and `SupportingPaneScaffold`. These scaffolds manage up to three functional panels: primary list, secondary detail, and tertiary contextual extra panes. The scaffold tracks hardware state: when operating on a standard compact phone, it displays a single pane, automatically routing back navigation to previous views. When the application expands on a tablet or an unfolded foldable, the scaffold displays list and detail panes side by side. On foldable hardware, the scaffold positions layout dividers directly across the physical hinge, ensuring critical interactive controls do not intersect the display crease. Navigation state across panes is governed by `ThreePaneScaffoldNavigator`, which manages predictive back gestures and pane transitions internally.

### Apple Platforms (iOS and macOS) Adaptive Implementations

Because the user interface is shared between iOS and macOS, the Apple UI codebase must implement a unified layout structure capable of responding to both touch-driven mobile orientation shifts and desktop cursor-driven window manipulations.

The primary container for this shared Apple interface is SwiftUI’s `NavigationSplitView`. On macOS and regular-width iPadOS screens, `NavigationSplitView` renders an integrated two-column or three-column hierarchy (Sidebar, Content, Detail). On a compact iPhone screen, `NavigationSplitView` automatically adapts, collapsing its columns into a single navigational stack with built-in push-pop transitions without requiring branching platform logic. For top-level routing, combining this container with `.tabViewStyle(.sidebarAdaptable)` provides platform adaptation by displaying a bottom tab bar on compact iOS screens while rendering an interactive sidebar on macOS and iPadOS.

A major architectural consideration unique to macOS is fluid, continuous window resizing. Mobile applications experience discrete layout changes upon device rotation, but desktop applications must handle arbitrary user dragging of window borders. SwiftUI views shared across iOS and macOS must avoid hardcoded layout frames, relying instead on relative constraints, flexible spacer mechanics, and balanced split view styles (`.navigationSplitViewStyle(.balanced)`) to ensure continuous visual reflow down to the platform's minimum dimensions.

### Structural Isolation of Physical Layout Geometry

A frequent anti-pattern in cross-platform development is leaking hardware display metrics—such as window widths, pixel dimensions, or hinge bounds—into shared business logic. The shared presentation core in `commonMain` must remain agnostic of physical geometry. The shared layer manages only semantic state, such as tracking which item ID is currently active or selected. The respective platform UI tiers independently evaluate local display metrics (`WindowAdaptiveInfo` on Android, `HorizontalSizeClass` on Apple platforms) to determine whether that selected item is presented in an adjacent pane or pushed onto a navigation stack.

### Cross-Platform Adaptive Matrix

| Form Factor | Physical Characteristics & Posture | Android (Jetpack Compose) Architecture | Apple (Shared SwiftUI iOS & macOS) Architecture |
| :--- | :--- | :--- | :--- |
| **Compact Phone** | Narrow portrait display; touch-primary. | `NavigableListDetailPaneScaffold` in single-pane mode; back gestures pop active pane. | `NavigationSplitView` collapsed into a single column; stack-based navigation. |
| **Foldable (Unfolded)** | Square or wide canvas; physical crease dividing display regions. | `ListDetailPaneScaffold` reading `Posture` and aligning pane margins to `HingeInfo`. | `NavigationSplitView` displaying dual columns with appropriate safe-area padding. |
| **Tablet** | Expanded touch canvas in landscape or portrait. | Dual- or triple-pane scaffold displaying list, detail, and optional supporting pane. | Two- or three-column `NavigationSplitView` with toggleable sidebar controls. |
| **macOS Desktop** | Arbitrary continuous window sizing; mouse, trackpad, and keyboard. | Target not deployed on desktop (handled natively by Darwin target). | Fluid `NavigationSplitView` responding to live window dragging and macOS menu bars. |

---

## Structural System Architecture and Execution Topologies

The structural architecture separates presentation, domain, and data access concerns across platform boundaries. The following structural breakdown details the functional tiers and the sequence of information exchange across the application runtime.

### Multi-Tiered System Structure

| Architectural Layer | Target Environment | Core Technologies & Components | Primary Responsibilities |
| :--- | :--- | :--- | :--- |
| **Layer 1: Native Presentation (Android)** | Android (`androidMain`) | Jetpack Compose, Material 3 Adaptive Scaffolds, Window Size Class APIs. | Renders Android UI; senses fold postures and screen classes; binds state via Compose runtime. |
| **Layer 1: Native Presentation (Apple)** | Apple (`appleMain` / Swift) | Shared SwiftUI, `NavigationSplitView`, Swift Observation (`@Observable`). | Renders iOS and macOS UI; handles desktop window resizing; adapts sidebar and stack layouts. |
| **Layer 2: Platform Interoperability** | Bridge (`iosMain`, `macosMain`) | Swift Export (`embedSwiftExportForXcode`), SPM Dependencies, or SKIE Linker. | Translates `StateFlow` to native `AsyncSequence`; bridges structured cancellation; exports enums via `.sealedType()`. |
| **Layer 3: Shared Presentation** | Cross-Platform (`commonMain`) | Decompose Components, Essenty Lifecycle, or KMP ViewModels. | Encapsulates screen state; orchestrates unidirectional data flow; preserves state across restarts. |
| **Layer 4: Shared Domain** | Cross-Platform (`commonMain`) | Domain Use Cases, Business Interactors, Kotlin Coroutines. | Implements core business logic, validation rules, and transactional domain coordination. |
| **Layer 5: Shared Data Access** | Cross-Platform (`commonMain`) | Room KMP Database, `androidx.sqlite` Driver, Ktor Client, Serialization. | Executes offline-first database caching, remote REST/JSON networking, and data mapping. |

### End-to-End Unidirectional Information Lifecycle

Runtime information processing follows a strict cyclical path that isolates state mutations from the UI rendering layer:

1. **User Interaction Event**: An input event occurs within the active UI. On Android, a user taps a record in a `ListPane` composable. On macOS or iOS, the user clicks or taps a corresponding item in SwiftUI's `NavigationSplitView` sidebar.
2. **Intent Propagation**: The native view does not execute logic directly. Instead, it dispatches a typed intent—such as `CatalogIntent.SelectRecord(id)`—into the shared presentation component (Decompose Component or ViewModel).
3. **Domain Execution Pipeline**: The presentation component receives the intent and delegates work to the appropriate shared domain Use Case. Execution shifts to background worker threads using `Dispatchers.Default` via structured coroutine builders.
4. **Data Layer Synchronization**: The Use Case interacts with the shared Repository. The Repository queries the local Room KMP database through the `androidx.sqlite` driver, immediately returning cached records as a reactive `Flow`, while simultaneously issuing a network request through the Ktor HTTP client to retrieve fresh updates from remote services.
5. **State Reduction and Emission**: As updated data arrives, the presentation component reduces the previous UI state and incoming data into a new, immutable `UiState` instance. This updated state contains semantic properties—such as the active record ID and detail payloads—without referencing screen dimensions or device posture.
6. **Platform UI Re-Rendering**:
   - **On Android**: Jetpack Compose observes the updated state via `collectAsStateWithLifecycle()`. If the device is unfolded or on a tablet, `NavigableListDetailPaneScaffold` populates the adjacent `DetailPane` while keeping the list visible; if on a compact phone, it triggers an animated slide transition to display the detail view full-screen.
   - **On Apple platforms**: Native Swift Export (or SKIE) delivers the updated state through an `AsyncSequence` to the Swift state holder. On macOS, `NavigationSplitView` updates the detail column while maintaining user-defined pane widths. On iPhone, the collapsed navigation container pushes the detail view onto the active `NavigationPath`.

---

## Architectural Recommendations for the Target Stack

To ensure maintainability, testability, and clean separation between shared business logic and native presentation, the application architecture should adopt a component-based Unidirectional Data Flow pattern centered on Decompose and Essenty, supported by first-party Swift Export (or SKIE) for Darwin interop and Room KMP for persistence.

### Implementation Framework Justification

While extending `androidx.lifecycle.ViewModel` directly in `commonMain` is viable for simpler multiplatform apps, it creates structural friction in this specific three-target deployment. AndroidX ViewModel does not possess native lifecycle integration on iOS or macOS, requiring manual memory management wrappers to clear coroutines when views dismiss.

Decompose eliminates this asymmetry. It organizes application logic into a hierarchical tree of decoupled components, where each node receives a `ComponentContext` providing lifecycle awareness, back-stack navigation, state saving (`StateKeeper`), and instance retention (`InstanceKeeper`) across configuration changes.

Because Decompose is platform-agnostic, the same component tree controls navigation logic across Android, iOS, and macOS. For adaptive layouts, Decompose provides `ChildPanels` navigation models that manage master-detail relationships in shared code while allowing Jetpack Compose and SwiftUI to render the panes using their respective platform scaffolds.

### Recommended Technology Stack

- **Architecture and Navigation Core**: Decompose accompanied by Essenty for unified lifecycle, state preservation, and back-button handling.
- **State Management Strategy**: Unidirectional Data Flow using MVIKotlin or FlowMVI within Decompose components to enforce immutable state transitions and deterministic testing.
- **Interoperability Pipeline**: First-party Swift Export (`embedSwiftExportForXcode`) or Touchlab SKIE to generate Swift-native interfaces for Kotlin flows, sealed classes via `.sealedType()`, and suspend functions.
- **Dependency & Package Management**: Kotlin Toolchain with Swift Package Manager (SPM) dependency integration for Darwin platforms.
- **Local Persistence Engine**: Room KMP backed by the official `androidx.sqlite` driver, enabling structured relational data caching across Android, iOS, and macOS.
- **Networking Infrastructure**: Ktor Client utilizing the Android/OkHttp engine for Android and the Darwin engine for iOS and macOS.
- **Dependency Injection**: Koin Core configured in shared code, exposing initialization hooks for Android Application classes and Swift entry points.
- **Android Presentation Layer**: Jetpack Compose using the Material 3 Adaptive library suite (`NavigableListDetailPaneScaffold`).
- **Apple Presentation Layer**: A single, shared SwiftUI package shared between iOS and macOS utilizing `NavigationSplitView` and `.tabViewStyle(.sidebarAdaptable)`.

### Strategic Evaluation of Alternative Architectural Stacks

| Architectural Configuration | Shared Logic Coverage | Darwin Lifecycle Hygiene | Adaptive Layout Interop | Engineering Complexity |
| :--- | :--- | :--- | :--- | :--- |
| **Recommended: Decompose + UDF (MVI) + Swift Export / SKIE** | **High**: Navigation, state machines, domain, and data fully shared. | **High**: Symmetrical lifecycle managed automatically by Essenty. | **High**: Shared component panels drive platform-native scaffolds. | **Moderate**: Requires familiarity with component hierarchies. |
| **Alternative A: AndroidX MVVM + Swift Export / SKIE** | **Moderate**: ViewModels, domain, and data shared; navigation duplicated. | **Medium**: Requires custom `ViewModelStoreOwner` or deinit hooks on Darwin. | **Moderate**: Selections exposed via flow; UI scaffolds manage layout logic. | **Low**: Standard Android lifecycle mental model. |
| **Alternative B: Separated ViewModels (Kotlin/Swift)** | **Low**: Only domain and data layers shared; presentation duplicated. | **High**: Native SwiftUI view models utilize native Swift lifecycles. | **Manual**: Developers must implement adaptive logic twice. | **Low initially / High long-term**: Ongoing divergence between platform features. |

### Strategic Implementation Guidelines

To execute this architecture effectively, engineering teams must maintain strict structural boundaries across development phases:

#### Shared Presentation Contracts

Presentation contracts in `commonMain` should define an immutable data class for UI state and a sealed interface for incoming intents. State containers must expose read-only streams (`Value<State>` in Decompose or `StateFlow<State>` in standard ViewModels). When consumed via Swift Export, sealed hierarchies map seamlessly through the generated `.sealedType()` accessor into native Swift enums. Internal updates should strictly use atomic operators, such as `_state.update { ... }`, preventing race conditions across concurrent coroutine jobs.

#### Swift Observation Adapter

Within the shared Apple codebase, state observation should be bridged using an adapter class decorated with the Swift 5.9+ `@Observable` macro. This adapter holds a reference to the shared Kotlin component, consumes its state stream via an asynchronous `Task` over the exported `AsyncSequence`, and assigns updates to local fields on the `@MainActor`. Leveraging `@Observable` ensures that SwiftUI tracks field-level access, re-rendering only the specific views reading a modified property rather than invalidating entire view trees upon every state change.

#### Adaptive Boundary Isolation

Engineers must prevent hardware metrics from crossing into common code. The shared layer tracks semantic selections, such as `selectedRecordId: String?`, while platform shells manage presentation:

- **On Android**: Jetpack Compose passes this ID to `rememberListDetailPaneScaffoldNavigator`, allowing `NavigableListDetailPaneScaffold` to position dividers over physical hinges and handle predictive back navigation automatically.
- **On iOS and macOS**: The shared SwiftUI root view binds this ID to a `NavigationSplitView`, enabling macOS to adjust column widths fluidly during window resizing while compact iPhones collapse the detail pane into a push-pop navigation stack.

#### Concurrency and Fault Boundaries

Shared business logic must receive injected coroutine dispatchers rather than referencing hardcoded dispatchers, enabling headless unit testing via `StandardTestDispatcher` and `runTest`. Furthermore, all public-facing suspend methods in the shared boundary must be marked with `@Throws` or return functional result wrappers, preventing unhandled Kotlin exceptions from terminating Darwin application processes. Where platform-specific capabilities (e.g., Apple hardware sensors or local keychains) must be injected into shared code, teams can use Swift Export's cross-language inheritance to implement shared Kotlin interfaces directly in Swift.

---

## Architectural Decision Record (ADR): Adoption of Pragmatic Shared MVI / UDF atop KMP ViewModel

### Context & Problem Statement
The image-feed-app initially utilized a custom Shared Presenter pattern across Kotlin Multiplatform (`shared/commonMain`) driving native UI shells in Jetpack Compose (Android) and SwiftUI (iOS / macOS). Presenters exposed an immutable `StateFlow<UiState>` but relied on ad-hoc public mutation methods (`loadNextPage()`, `refresh()`, `selectTopic()`), a manual `PresenterScope` / `PresenterScopeFactory` tied to a custom `fun clear()` teardown contract, and an Android proxy wrapper `PresenterViewModel`.

While the research blueprint above considered a heavy greenfield stack based on Decompose + Essenty + MVIKotlin, adopting full Decompose in this repository introduces severe architectural friction:
1. It directly clashes with the established and hardened **Jetpack Navigation 3** + **Material 3 Adaptive** architecture on Android (`androidx.navigation3`, `androidx.compose.material3.adaptive`), which is a non-negotiable core invariant (`AGENTS.md`).
2. It conflicts with compile-time dependency injection managed via **Metro** (`dev.zacsweers.metro`), which enforces compile-time graph safety rather than dynamic runtime component hierarchy resolution.

### Decision
The project adopts **Option B: Pragmatic Shared MVI / Unidirectional Data Flow (UDF) built atop `androidx.lifecycle.ViewModel` in `commonMain`**.

Under this model:
- **Base State Machine**: Shared components extend official multiplatform `androidx.lifecycle.ViewModel`. Coroutines run in the standard `viewModelScope` and teardown is governed by `onCleared()`.
- **Unidirectional Data Flow**:
  - **State**: Single, immutable data class exposed as `StateFlow<UiState>`.
  - **Intent**: Single entry point `fun dispatch(intent: UiIntent)` handling typed user actions (sealed interface).
  - **Effect**: Dedicated `Flow<UiEffect>` for one-off side effects (navigation triggers, haptic feedback, transient errors/toasts) decoupled from persistent UI state.
- **Darwin Interop**: Swift view models (annotated with `@Observable`) observe `viewModel.state` and route user interactions into `viewModel.dispatch(intent:)`. Sealed Kotlin intents map cleanly to native Swift enums via Swift Export / SKIE.
- **Android Interop**: Jetpack Compose views observe state natively via `collectAsStateWithLifecycle()` and dispatch intents directly to the ViewModel scoped within Navigation 3 back-stack entries.

### Trade-offs & Rationales

| Dimension | Positive Trade-offs (Gains) | Negative Trade-offs (Costs) & Mitigations |
| :--- | :--- | :--- |
| **Lifecycle & Concurrency** | Eliminates custom `PresenterScope`, `PresenterScopeFactory`, and Android `PresenterViewModel` proxies. Leverages official multiplatform `viewModelScope`. | Darwin targets require explicit scope cancellation on view disposal (`viewModel.clear()` in Swift `deinit`). *Mitigated by standard `@Observable` wrapper patterns.* |
| **State Consistency** | Replaces disparate public mutation methods with a single `dispatch(intent)` pipeline, eliminating race conditions and torn reads. Separates one-off events into `effects`. | Introduces sealed interface ceremony (`Intent`, `Effect`) for each screen. *Mitigated by lightweight contract definitions.* |
| **Navigation & DI Stability** | 100% preservation of Jetpack Navigation 3, Material 3 Adaptive Scaffolds, and Metro compile-time DI. Zero architectural regression risk. | Navigation remains platform-specific (Navigation 3 on Android, `NavigationSplitView` on iOS/macOS) rather than unified in shared code. *Acceptable since platform UI shells are already decoupled.* |
| **Migration Risk** | Evolution rather than rewrite: Presenters already held consolidated `UiState`; migrating to MVI `ViewModel` is incremental, screen-by-screen. | Transitory period where legacy Presenters and new MVI ViewModels coexist in the codebase. *Mitigated by phased migration with explicit transition state guidelines in `AGENTS.md`.* |

