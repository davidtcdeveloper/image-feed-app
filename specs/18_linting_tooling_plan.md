# Linting and Static Analysis Toolchain Plan

This plan adds a comprehensive, consistent quality-feedback toolchain for both Kotlin and Swift code in the project.

## Objectives

1. Give Kotlin code a strong first-pass style and static-analysis layer.
2. Give Swift code the same level of quality feedback in Xcode and CI.
3. Capture a baseline of existing violations before tightening rules.
4. Fix pre-existing issues in an explicit iteration so the new tools are usable immediately.

## Recommended Toolchain

### Kotlin
- ktlint: style and formatting enforcement for Kotlin source.
- detekt: deeper static analysis for complexity, smells, duplicates, and maintainability.

### Swift
- SwiftLint: linting and style rules for Swift/SwiftUI code.
- SwiftFormat: formatting and style normalization for Swift code.

This combination gives the broadest practical coverage without overloading the project with conflicting rule sets.

## Implementation Phases

### Phase 1: Baseline Inventory
1. Add the tooling configuration files without enforcing strict fail-on-violation yet.
2. Run the tools in report-only mode to capture current findings.
3. Save the baseline report for Kotlin and Swift in the project notes or CI artifacts.
4. Classify issues into:
   - auto-fixable
   - safe to fix now
   - potentially risky / requires design review

### Phase 2: Kotlin Tooling Setup
1. Add ktlint to Gradle using the existing version catalog.
2. Add detekt to Gradle using the same catalog approach.
3. Configure rule sets for:
   - style and formatting
   - complexity / long methods
   - naming and maintainability
   - duplicate code warnings
4. Run ktlint and detekt on shared and androidApp code.
5. Review generated findings and choose the first safe rule set to enforce.

### Phase 3: Swift Tooling Setup
1. Add SwiftLint configuration for iosApp and macosApp sources.
2. Add SwiftFormat configuration to normalize formatting consistently.
3. Run the tools against the Swift target and capture findings.
4. Keep the initial rule set conservative so the project does not fail on unrelated legacy code.

### Phase 4: Pre-existing Issue Remediation Iteration
1. Fix auto-fixable issues first.
2. Fix low-risk style and naming issues in Kotlin and Swift.
3. Review higher-risk detekt findings and fix the underlying issue when it is clearly beneficial.
   - Do not reduce rule severity to make the tool pass.
   - The project quality bar must not be lowered to accommodate existing issues.
4. Re-run the tools after each batch of fixes to confirm progress.
5. Document any remaining intentional exceptions in the lint configuration.

### Phase 5: CI and Developer Workflow Integration
1. Add Gradle tasks for:
   - ktlintCheck
   - detekt
2. Add SwiftLint/SwiftFormat tasks or Xcode/CI hooks for Apple targets.
3. Make the tooling run in CI so feedback is consistent for all contributors.
4. Add a simple developer command path for local execution.
5. Update `AGENTS.md` with the lint/static-analysis commands and a policy that agents must run the relevant tools after changes and fix reported issues before considering the work complete.

## Acceptance Criteria

- Kotlin and Swift lint/static-analysis tools are configured in the project.
- A baseline report is generated before strict enforcement.
- Pre-existing issues are analyzed and fixed in an explicit remediation pass.
- The workflow is repeatable in local development and CI.

## Notes

- Start with conservative rules to avoid blocking unrelated work.
- Prefer deterministic, widely adopted tools over custom-only solutions.
- Keep Kotlin and Swift tool configurations aligned with the existing Gradle and Apple build setup.
