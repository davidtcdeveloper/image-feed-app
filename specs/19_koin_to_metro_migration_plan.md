# Spec 19: Transitioning from Koin to Metro Dependency Injection

## Goal
Migrate the application from **Koin** (a runtime service locator) to **Metro** (a compile-time dependency injection framework implemented as a native Kotlin compiler plugin) to achieve compile-time verification, cycle detection, and direct-invocation runtime performance.

---

## Technical Evaluation
1. **Compile-time Graph Construction**: Metro operates directly on the Frontend Intermediate Representation (FIR) and Intermediate Representation (IR) phases of the K2 compiler, avoiding slow KSP/KAPT rounds and verifying safety before bytecode lowering.
2. **DoubleCheck Provider Locking**: Scoped dependencies use Double-Check double-locking factory implementations, which match manual constructor allocations without the map lookup/mutex overhead of Koin.
3. **Assisted Injection**: Transition parameterized presenters from Koin's dynamic factory closures (`factory { (id: String) -> Presenter(get(), id) }`) to Metro's `@Assisted` and `@AssistedFactory` definitions.
4. **Visiblity Restrictions**: Ensure all aggregated bindings (annotated with `@ContributesBinding`) are marked `public` instead of `internal` to prevent cross-module aggregation visibility errors.

---

## Component Mapping from Koin to Metro

| Koin DI API | Metro DI Construct | Explanation |
| :--- | :--- | :--- |
| `single { Impl() }` | `@SingleIn(Scope::class) + @Inject` | Double-checked singleton provider factory for scoped lifetime. |
| `factory { Impl() }` | Unscoped class with `@Inject` | Creates a new instance on every request. |
| `single<Interface> { Impl() }` | `@ContributesBinding(Scope::class)` | Statically registers concrete implementation for interface lookup. |
| `get()` | Explicit constructor parameters | Parameter types are auto-resolved and verified by the compiler. |
| `by inject()` | `Lazy<T>` / Direct property lookup | Defer resolution until access or resolve directly from the Graph. |
| `factory { (param: Type) -> Impl(param) }` | `@AssistedFactory` | Generates a compile-time factory interface for dynamic parameters. |
| `module { ... }` | `@BindingContainer` | Declares modules for external types or manual factory functions. |

---

## Proposed Solution: Phase-by-Phase Plan

### Phase 1: Setup & Coexistence Bridge
We will run both Metro and Koin in parallel to allow incremental migration of presenters and view controllers.
1. **Configure Gradle & Versions**:
   - Add `dev.zacsweers.metro` to `gradle/libs.versions.toml`.
   - Apply the plugin in `shared/build.gradle.kts` and root `build.gradle.kts`.
   - Enable `metro { enabled = true }` in the DSL block.
2. **Define Base Graph & Scoping**:
   - Create `@Scope annotation class AppScope` in `shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt`.
   - Declare `@BindingContainer interface NetworkModule` to provide `HttpClient`.
   - Create `@DependencyGraph(scope = AppScope::class, bindingContainers = [NetworkModule::class]) interface ApplicationGraph` exposing non-DI entry points.
3. **Create the Coexistence Bridge**:
   - Initialize the Graph inside a global helper object:
     ```kotlin
     object MetroHelper {
         val graph: ApplicationGraph by lazy {
             createGraph<ApplicationGraph>()
         }
     }
     ```
   - Delegate Koin's `commonModule` singleton registrations to `MetroHelper.graph` (e.g., `single { MetroHelper.graph.unsplashRepository }`).

### Phase 2: Systematic Presenter & View Migration
Convert presenters to construct injection and adjust platforms to fetch from the new compile-time graph.
1. **Unparameterized Presenters**:
   - Add `@Inject constructor(...)` to `FeedPresenter`, `RandomPhotoPresenter`, `UnifiedSearchPresenter`, and `CollectionsFeedPresenter`.
2. **Parameterized Presenters (Assisted Injection)**:
   - For `PhotoDetailsPresenter`, `CollectionDetailPresenter`, and `UserProfilePresenter`, annotate constructors with `@Inject` and parameters with `@Assisted`.
   - Define a companion or separate `@AssistedFactory` interface for each, exposing a `create()` method.
3. **Expose to Platforms**:
   - Expose the presenters and factories through `MetroHelper` (replacing `KoinHelper`).
   - Change `MainActivity.kt` and all SwiftUI ViewModels to use `MetroHelper` instead of Koin's `by inject()`.

### Phase 3: Cleanup and Compile-time Diagnostics
1. Remove `KoinHelper` and Koin initialization in `ImageFeedApplication.kt` and `iOSApp.swift`.
2. Remove Koin dependencies from Version Catalogs and Gradle scripts.
3. Enable `desugaredProviderSeverity = "ERROR"` in `metro { ... }` configuration block to enforce strict compile-time safety and prevent fallback issues.
4. Run full code compilation and style checks (`./gradlew ktlintCheck detekt`).

---

## Acceptance Criteria
- Codebase builds successfully for Android, iOS, and macOS platforms.
- Complete dependency graph validation occurs at compile-time.
- All dynamic parameter presenters resolve correctly using `@AssistedFactory`.
- Zero Koin runtime references remain in `build.gradle.kts` and source classes.

---

## Verification Plan
1. **Build Verification**:
   - `./gradlew assembleDebug`
   - `./gradlew :shared:compileKotlinIosSimulatorArm64`
2. **Static Analysis & Formatting**:
   - `./gradlew ktlintCheck detekt`
3. **Runtime Verification**:
   - Verify infinite scroll pre-fetching, photographer attribution links, and download tracking still function perfectly across both platforms with Metro DI.
