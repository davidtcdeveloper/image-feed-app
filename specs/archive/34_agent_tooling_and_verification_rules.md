# Specification: Agent Tooling, Sandbox Execution & Verification Modernization

**Status:** Implemented

## 1. Overview & Objectives

Recent operational evaluations of the agent guidance architecture (`AGENTS.md`, `ai-rules/`, and associated tooling) identified significant workflow friction points that hinder AI developer agents from reliably executing builds, running tests, resolving static analysis issues, and utilizing available tools.

Key friction points include:
1. **Sandbox vs. Host Tooling Disconnect**: Build commands (`./gradlew`, `xcodebuild`, `xcodegen`) fail when executed inside the standard container sandbox due to hard dependencies on external directories (`JAVA_HOME`, Android SDK, `~/.gradle`, Xcode toolchain).
2. **Phantom Tool References**: Mentions of nonexistent tools like `analyze_file` lead agents to invalid tool calls.
3. **SwiftFormat Gaps & Baseline Violations**: Absence of the automated `swiftformat .` formatting command and the presence of 11 pre-existing Swift formatting violations that block strict pre-commit lint gates.
4. **Dynamic Rule Routing Incompleteness**: `ai-rules/rule-loading.md` omits `build-and-deps.md` during iOS/macOS UI tasks (preventing agents from learning XcodeGen/xcodebuild commands) and lacks routes for build failure diagnostics and lint verification.
5. **Testing Guidance Scaffolding Omissions**: `ai-rules/testing.md` does not document the existing shared presenter test scaffolding (`TestPresenterScopeFactory`, `TestDispatcherProvider`, `FakeUnsplashRepository`) or the absence of an iOS test target.
6. **Xcode MCP Server vs. CLI Disconnect**: Ambiguity between the lazy `xcode` MCP server and project-mandated `xcodegen` generation.

This specification details the end-to-end plan to rectify these operational deficiencies across the repository's rule sets, tool guidance, and Swift source baselines.

---

## 2. Identified Inconsistencies & Detailed Technical Specifications

### A. Sandbox Execution & Environment Permissions
* **Issue**: The agent runtime executes shell commands inside a restricted sandbox (`BypassSandbox: false`) by default. This sandbox isolates filesystem access exclusively to `/Users/davidtiagoconceicao/Developer/image-feed-app`. However:
  * JDK runtime is located at `/Users/davidtiagoconceicao/Library/Java/JavaVirtualMachines/...`
  * Android SDK is located at `~/Library/Android/sdk`
  * Gradle daemon caches and metadata reside in `~/.gradle`
  * Xcode toolchains and SDKs are housed in `/Applications/Xcode.app`
  Executing `./gradlew :shared:allTests` or `./gradlew :androidApp:assembleDebug` inside the sandbox terminates with:
  `ERROR: JAVA_HOME is set to an invalid directory: /Users/.../openjdk-25.0.1/Contents/Home`
* **Remediation**:
  * Update `AGENTS.md` and `ai-rules/build-and-deps.md` with an explicit Sandbox Execution Policy:
    Instruct agents that build, test, and code-analysis commands (`./gradlew`, `xcodebuild`, `xcodegen`, `swiftlint`, `swiftformat`) require host execution permissions (`BypassSandbox: true`) because they interact with external system SDKs and JDK installations.
  * Standard read/write operations on workspace files, directory scans, and git commands remain fully functional in standard sandboxed mode.

### B. Diagnostics & Compiler Warning Resolution
* **Issue**: `AGENTS.md` under the "Zero Warning Policy" directs agents to:
  *"run `analyze_file` on modified files or execute a full build to identify new warnings or deprecations."*
  `analyze_file` is not an agent tool in the environment.
* **Remediation**:
  * Replace the phantom `analyze_file` directive with explicit, deterministic command sequences:
    1. For Kotlin/Shared code: Execute `./gradlew :shared:compileKotlinIosSimulatorArm64` or `./gradlew :androidApp:compileDebugKotlin` and inspect compiler diagnostic output.
    2. For Swift/Apple code: Execute `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO` and check compiler warnings.
    3. For Lint/Static Analysis: Run `./gradlew ktlintCheck detekt` and `swiftlint lint iosApp/iosApp`.

### C. Swift Formatting & Baseline Cleanup
* **Issue**:
  * `AGENTS.md` commands only list `swiftformat --lint .`, omitting the auto-format command `swiftformat .`.
  * Running `swiftformat --lint .` currently fails across 11 Swift files (`ContentView.swift`, `PhotoDetailsView.swift`, etc.) due to indentation, trailing commas, line wrapping, and redundant `@ViewBuilder` attributes. This trips the "Zero Warning / All Lints Pass" requirement even for unrelated changes.
* **Remediation**:
  * Document `swiftformat .` as the primary Swift auto-formatting command in `AGENTS.md` and `ai-rules/build-and-deps.md`.
  * Run `swiftformat .` to reformat the repository's Swift codebase to establish an unsoiled baseline where `swiftformat --lint .` passes cleanly.

### D. Dynamic Rule Loading Routing (`ai-rules/rule-loading.md`)
* **Issue**:
  * Line 25 of `ai-rules/rule-loading.md` routes iOS/macOS tasks to `design-principles.md`, `apple-design.md`, `architecture.md`, and `specs-and-commits.md`. It neglects `build-and-deps.md`, leaving agents unaware of the XcodeGen synchronization requirement and build verification command.
  * There are no routing entries for diagnostic debugging, test scaffolding, or static analysis runs.
* **Remediation**:
  * Update `ai-rules/rule-loading.md` quick reference table:
    * **iOS/macOS UI & Swift Work**: Load `build-and-deps.md`, `apple-design.md`, `design-principles.md`, `architecture.md`, and `specs-and-commits.md`.
    * **Diagnosing Build Failures & Warnings**: Load `build-and-deps.md` and `architecture.md`.
    * **Linting & Code Formatting**: Load `build-and-deps.md` and `git-guidelines.md`.
    * **Adding or Updating Tests**: Load `testing.md`, `architecture.md`, and `build-and-deps.md`.

### E. Testing Strategy & Scaffolding Standards (`ai-rules/testing.md`)
* **Issue**:
  * `ai-rules/testing.md` instructs adding integration tests under `shared/src/commonTest/...` using fake repositories, but provides no concrete reference to the project's DI-driven coroutine test harness.
  * Presenters in this repository require `PresenterScopeFactory` and `DispatcherProvider`. In tests, developers must instantiate `TestPresenterScopeFactory` and `TestDispatcherProvider`.
  * The rule does not mention the platform test boundaries (lack of an iOS test target in `project.yml` vs. shared KMP tests).
* **Remediation**:
  * Expand `ai-rules/testing.md` to document the canonical presenter test recipe:
    ```kotlin
    val testDispatcher = StandardTestDispatcher()
    val dispatcherProvider = TestDispatcherProvider(testDispatcher)
    val presenterScopeFactory = TestPresenterScopeFactory(dispatcherProvider)
    val repository = FakeUnsplashRepository()
    val presenter = FeedPresenter(repository, presenterScopeFactory, dispatcherProvider)
    ```
  * Note that platform UIs are thin declarative shells without dedicated platform test targets, making `shared/src/commonTest` (`./gradlew :shared:allTests`) the primary regression boundary.

### F. Tooling & MCP Integration Guidelines
* **Issue**:
  * Agents have access to an `xcode` MCP server (`BuildProject`, `RunProject`, `RunAllTests`, etc.).
  * `iosApp.xcodeproj` is generated dynamically from `iosApp/project.yml` via XcodeGen. Calling Xcode MCP tools or `xcodebuild` without regenerating risks building against outdated scheme settings or missing newly added Swift files.
* **Remediation**:
  * Add a dedicated "Tooling & MCP Interaction Guidelines" section in `AGENTS.md` and `ai-rules/build-and-deps.md`:
    * When interacting with Apple targets via CLI or Xcode MCP tools, `xcodegen generate --spec iosApp/project.yml` MUST be executed first whenever project files, sources, or settings change.
    * For headless terminal verification, prefer the canonical non-interactive `xcodebuild` command.

---

## 3. Implementation Plan & Execution Checklist

- [x] **Phase 1: Modernize Agent Root Instructions (`AGENTS.md`)**
  - [x] Add the Sandbox Execution note explaining the requirement for `BypassSandbox: true` on build/test/lint commands.
  - [x] Remove `analyze_file` reference and replace with explicit compiler verification commands.
  - [x] Add `swiftformat .` auto-formatting command alongside `swiftformat --lint .`.
  - [x] Add XcodeGen-first policy for Xcode CLI / MCP interactions.
  - [x] Update Reference Specs Directory to include `specs/34_agent_tooling_and_verification_rules.md`.

- [x] **Phase 2: Update Dynamically Loaded Rules (`ai-rules/`)**
  - [x] Update `ai-rules/rule-loading.md` with complete routing (include `build-and-deps.md` for iOS/macOS tasks; add routing for build diagnostics, linting, and test authoring).
  - [x] Update `ai-rules/build-and-deps.md` with Sandbox guidance, `swiftformat .` instructions, and XcodeGen generation rules.
  - [x] Update `ai-rules/testing.md` with presenter test harness recipe (`TestPresenterScopeFactory`, `TestDispatcherProvider`, `FakeUnsplashRepository`) and platform test target clarity.

- [x] **Phase 3: Clean SwiftFormat Baseline Violations**
  - [x] Run `swiftformat .` across the workspace to resolve existing formatting violations in `iosApp/`.
  - [x] Verify that `swiftformat --lint .` exits with code 0.

- [x] **Phase 4: Full Toolchain Verification**
  - [x] Verify `./gradlew :shared:allTests` passes.
  - [x] Verify `./gradlew :androidApp:assembleDebug` passes.
  - [x] Verify `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO` passes.
  - [x] Verify `./gradlew ktlintCheck detekt` passes.
  - [x] Verify `swiftlint lint iosApp/iosApp` passes.
  - [x] Verify `swiftformat --lint .` passes.

- [x] **Phase 5: Steps & Spec Status Finalization**
  - [x] Update `specs/steps.md` with Step 36.
  - [x] Update `specs/steps.md` specification index.
  - [x] Mark this specification as `Implemented` upon completion.
