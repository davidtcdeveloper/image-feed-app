---
name: localization
description: >-
  Multiplatform localization guidelines, typed AppError mapping, platform string tables,
  native number/date formatters, and RTL directional layout standards across Android and Apple platforms.
  Use when adding or refactoring UI strings, errors, plurals, or formatted metrics.
---

# Multiplatform Localization Architecture Runbook

## Purpose & Triggers

This skill defines cross-platform localization rules, string extraction standards, typed error mapping, and layout bidirectional mirroring for Android and Apple platforms.
Use this skill when:
- Adding or modifying UI strings, dialogs, snackbars, or content descriptions in Android or iOS.
- Propagating and presenting error messages across UI boundaries.
- Formatting counts, statistics, metrics, or timestamps.
- Handling pluralization or parameter interpolation.
- Implementing navigation or directional iconography (RTL support).

---

## 1. Zero Hardcoded Strings Policy

- Hardcoded user-visible text in Compose or SwiftUI views is strictly prohibited.
- All user-facing labels, titles, error descriptions, action buttons, and accessibility descriptions must be externalized to platform-native string tables:
  - **Android**: [`androidApp/src/main/res/values/strings.xml`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/res/values/strings.xml) and [`plurals.xml`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/res/values/plurals.xml).
  - **Apple**: Xcode String Catalog at [`iosApp/iosApp/Localizable.xcstrings`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/Localizable.xcstrings).
- **Exceptions**: API query strings (e.g., Unsplash curated topic slugs/queries such as `"nature"`, `"travel"`, `"architecture"`), system logging, analytics identifiers, and SF Symbol / Material icon names remain non-localized internal tokens.

---

## 2. Typed Error Mapping Pattern

Errors originate from shared business logic as strongly-typed domain representations, never as raw, unlocalized strings.
- **Shared Model**: Sealed class [`AppError`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/core/model/src/commonMain/kotlin/com/example/imagefeed/model/AppError.kt) defines typed failure categories: `Network`, `Timeout`, `NotFound`, `Server(code)`, and `Unknown(rawMessage)`.
- **Shared Mapping**: Repositories map network/HTTP exceptions through [`AppErrorMapper`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/core/network/src/commonMain/kotlin/com/example/imagefeed/core/network/AppErrorMapper.kt).
- **Android Presentation**: Screen composables and view models resolve user-facing text via extension functions in [`AppErrorLocalization.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/AppErrorLocalization.kt) (`@Composable fun AppError.localizedMessage()` and `fun AppError.localizedMessage(context: Context)`).
- **Apple Presentation**: Swift view models and SwiftUI views resolve user-facing text via extension property `AppError.localizedMessage` in [`AppErrorLocalization.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/AppErrorLocalization.swift).

---

## 3. Parameter Interpolation & Pluralization

- **Positional Tokens**:
  - Android XML uses positional tokens: `%1$s` (strings), `%1$d` (integers), `%1$f` (floats).
  - Apple String Catalogs use typed positional specifiers: `%@` (strings), `%lld` (integers), `%f` (floats).
- **Plurals**:
  - Android: Use `<plurals>` in [`plurals.xml`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/res/values/plurals.xml) accessed via `pluralStringResource(id, count, count)`.
  - Apple: Configure plural variations (`zero`, `one`, `other`) directly within [`Localizable.xcstrings`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/Localizable.xcstrings) referenced via `String(localized: "%lld Photos")`.

---

## 4. Number & Date Formatting

Do not implement custom number abbreviation logic (e.g., ad-hoc divisions by 1,000 or 1,000,000). Always use platform-native, locale-aware formatters:
- **Android**:
  - Numbers: `android.icu.text.CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)`.
  - Dates: `java.time.format.DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)`.
- **Apple**:
  - Numbers: Swift native formatting `.formatted(.number.notation(.compactName))` or `.formatted(.number)`.
  - Dates: `date.formatted(date: .abbreviated, time: .omitted)` with `ISO8601DateFormatter`.

---

## 5. Bidirectional Layouts & Directional Icons

- **Mirroring Navigation**: Use semantic, directional icons that automatically mirror in Right-to-Left (RTL) locales.
  - Apple: Use SF Symbol `chevron.backward` (never hardcode `chevron.left` for back navigation).
  - Android: Use `Icons.AutoMirrored.Filled.ArrowBack` or ensure standard back button icons mirror automatically.
- **Directional Spacing**: Rely on `Start` and `End` padding/margins rather than hardcoded `Left` and `Right`.
