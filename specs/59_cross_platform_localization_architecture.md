# Cross-Platform Localization Architecture (Phase 1: English Extraction & Foundation)

**Status:** Implemented

## Overview

This specification establishes the architectural foundation and extraction plan for full localization across Android, iOS, macOS, and the shared Kotlin Multiplatform (KMP) business tier.

In accordance with project scope decisions:
1. **Target Language Selection (Phase 1)**: Extraction is performed exclusively in **English (`en`)** during this foundational phase to create a complete, centralized, and verified string catalog and resource architecture across all platforms before introducing secondary translations.
2. **Search Tag Query Translation**: Search tags and query terms (`"nature"`, `"travel"`, `"architecture"`, etc.) remain hardcoded in English for this phase. Dynamic query mapping between user-facing localized labels and Unsplash API indexing will be addressed in a future dedicated specification.
3. **Architecture Strategy**: Adheres strictly to **Platform-Native Resources paired with Shared Typed Domain Errors**. Because the app uses native SwiftUI on Apple platforms and Jetpack Compose on Android (rather than Compose Multiplatform UI), platform-native resource frameworks (Android `strings.xml`/`plurals.xml` and Apple `.xcstrings` String Catalogs) offer zero external dependency overhead, seamless OS toolchain integration, and flawless accessibility.

---

## Technical Root Cause & Design Solution

### 1. Shared Domain Error Modeling & ViewModel Decoupling
Currently, shared MVI ViewModels construct hardcoded English error strings in exception catch blocks (e.g., `e.message ?: "Failed to refresh collections"`), directly emitting raw strings into `UiState.error` and `UiEffect.ShowError`. Furthermore, [`SearchState.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/feature/search/src/commonMain/kotlin/com/example/imagefeed/presentation/search/SearchState.kt#L31) bundles hardcoded English mock terms in its initial state.

* **Solution**:
  1. Define a centralized, typed error hierarchy in `:core:model` (or `:core:mvi`):
     ```kotlin
     package com.example.imagefeed.model

     sealed class AppError {
         data object Network : AppError()
         data object Timeout : AppError()
         data object NotFound : AppError()
         data class Server(val code: Int) : AppError()
         data class Unknown(val rawMessage: String? = null) : AppError()
     }
     ```
     *(Note: `sealed class` is chosen over `sealed interface` because Kotlin/Native exports it as a clean Objective-C class hierarchy with typed subclasses for Swift pattern matching).*
  2. Implement a centralized exception mapper in `:core:network`:
     ```kotlin
     fun Throwable.toAppError(): AppError = when (this) {
         is io.ktor.client.network.sockets.SocketTimeoutException,
         is io.ktor.client.plugins.HttpRequestTimeoutException,
         is io.ktor.client.network.sockets.ConnectTimeoutException -> AppError.Timeout
         is io.ktor.client.plugins.ClientRequestException -> {
             if (response.status.value == 404) AppError.NotFound else AppError.Server(response.status.value)
         }
         is io.ktor.client.plugins.ServerResponseException -> AppError.Server(response.status.value)
         else -> AppError.Network
     }
     ```
  3. Refactor all 7 feature ViewModels (`FeedViewModel`, `CollectionsFeedViewModel`, `CollectionDetailViewModel`, `UnifiedSearchViewModel`, `PhotoDetailsViewModel`, `UserProfileViewModel`, `RandomPhotoViewModel`) to emit `AppError` instead of English strings.
  4. Change default `searchHistory` in [`SearchState.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/feature/search/src/commonMain/kotlin/com/example/imagefeed/presentation/search/SearchState.kt#L31) to `emptyList()`.
  5. Update `commonTest` suites (`PhotoDetailsViewModelTest.kt`, `CollectionsFeedViewModelTest.kt`, `UserProfileViewModelTest.kt`, `RandomPhotoViewModelTest.kt`) to assert typed `AppError` instances instead of raw English string literals.

### 2. Android Platform Localization Architecture (`:androidApp`)
[`androidApp/src/main/res`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/res) currently lacks `values/strings.xml` and `values/plurals.xml`. All Compose screens render hardcoded text literals.

* **Solution**:
  1. Create `androidApp/src/main/res/values/strings.xml` containing the complete 85-key unified catalog.
  2. Create `androidApp/src/main/res/values/plurals.xml` for pluralized photo counters:
     ```xml
     <resources>
         <plurals name="collections_photos_count">
             <item quantity="one">%1$d Photo</item>
             <item quantity="other">%1$d Photos</item>
         </plurals>
     </resources>
     ```
  3. **API Level Safety for Compact Numbers**:
     `androidApp` has `minSdk = 29`. `java.text.CompactNumberFormat` was only introduced in Android 14 (API 34) and will crash with `NoClassDefFoundError` on API 29–33.
     Replace `formatStatValue` in [`PhotoDetailsScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/PhotoDetailsScreen.kt#L1660) with Android ICU `CompactDecimalFormat` (available since API 24):
     ```kotlin
     fun formatStatValue(value: Int): String {
         val formatter = android.icu.text.CompactDecimalFormat.getInstance(
             java.util.Locale.getDefault(),
             android.icu.text.CompactDecimalFormat.CompactStyle.SHORT
         )
         return formatter.format(value.toLong())
     }
     ```
  4. **Locale-Aware Timeline Dates**:
     In [`UserProfileScreen.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/UserProfileScreen.kt#L1088), replace raw ISO string output (`YYYY-MM-DD`) with localized formatting using `java.time.LocalDate` and `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)`.
  5. **MVI Error Presentation**:
     Provide an `@Composable` extension mapping `AppError` to string resources (`R.string.error_network`, `R.string.error_timeout`, `R.string.error_server`, etc.).
  6. **Sensor Shake Normalization**:
     Update `handleShake()` in [`MainActivity.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/MainActivity.kt#L308) to use localized string resources for user-facing Toast messages.

### 3. Apple Platforms Localization Architecture (`:iosApp` & `macosApp`)
[`iosApp`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp) currently contains no string catalogs or localization files.

* **Solution**:
  1. **Configure XcodeGen (`iosApp/project.yml`)**:
     Add `developmentLanguage: en` and declare `knownRegions: [en]` under `options`:
     ```yaml
     options:
       bundleIdPrefix: com.example.imagefeed
       developmentLanguage: en
       knownRegions: [en]
     ```
  2. **Create Xcode String Catalog (`iosApp/iosApp/Localizable.xcstrings`)**:
     Add modern JSON-based string catalog containing manual extractions with English values.
  3. **Plural Handling in String Catalogs**:
     Define format keys such as `"%lld Photos"` with explicit plural variations (`one`: `"%lld Photo"`, `other`: `"%lld Photos"`). In SwiftUI, consume directly via `Text("\(count) Photos")`.
  4. **Swift Error Mapping**:
     Implement a Swift extension on `shared.AppError` providing `var localizedMessage: String` that resolves against `Localizable.xcstrings`.
  5. **Foundation Compact Number & Date Formatting**:
     Replace custom `%.1fM`/`%.1fK` string formatters in [`PhotoDetailsView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift#L650) and [`UserProfileView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L499) with Swift Foundation native formatting:
     ```swift
     func formatMetric(_ value: Int32?) -> String {
         guard let value else { return "--" }
         return Int(value).formatted(.number.notation(.compactName))
     }
     ```
     Format timeline sparkline dates using `ISO8601DateFormatter` and `date.formatted(date: .abbreviated, time: .omitted)`.
  6. **RTL Directional SF Symbols**:
     Replace hardcoded `chevron.left` back icons in [`PhotoDetailsView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsView.swift#L112), [`CollectionDetailView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/CollectionDetailView.swift#L252), and [`UserProfileView.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/UserProfileView.swift#L108) with auto-mirroring `chevron.backward`.

### 4. Agent Guidelines & Skill Governance (`AGENTS.md`, `GEMINI.md`, `.agents/skills/localization/SKILL.md`)
To guarantee that localization is systematically upheld across all future development, agent instructions and workspace skills must be updated:

1. **Modular Agent Skill (`.agents/skills/localization/SKILL.md`)**:
   In adherence to the repository's progressive disclosure policy (`RULE[user_global]`), detailed procedural runbooks, format token rules (`%1$d` vs `%lld`), plural catalog definitions, and RTL design rules must be packaged into a dedicated workspace skill:
   * **Name**: `localization`
   * **Location**: `.agents/skills/localization/SKILL.md`
   * **Content**:
     * Triggers and workflows for introducing new text, modifying existing strings, or authoring error states.
     * Platform conventions: Android `strings.xml` / `plurals.xml` with `stringResource()` / `pluralStringResource()` vs Apple `Localizable.xcstrings` String Catalogs with `String(localized:)` and `Text`.
     * Pluralization rules (`zero`, `one`, `other`) and parameter positional specifiers (`%1$s`, `%1$d`).
     * `AppError` translation extension patterns for Android and Apple.
     * Number, currency, and date formatting standards (Android ICU `CompactDecimalFormat`, Swift Foundation `.formatted()`).
     * RTL directional icon mapping (`chevron.backward` instead of `chevron.left`).

2. **Root Rule Invariants (`AGENTS.md` & `GEMINI.md`)**:
   Root instruction files must remain concise (< 75 lines):
   * **`AGENTS.md` Workspace Skills**: Add entry linking to `[Cross-Platform Localization](.agents/skills/localization/SKILL.md)`.
   * **`AGENTS.md` Core Invariants**: Add Invariant 5:
     * All user-facing text, accessibility content descriptions, plural counters, and error states must be defined in platform catalogs (`strings.xml`/`plurals.xml` or `Localizable.xcstrings`).
     * Shared ViewModels must never emit localized/raw English strings; they must emit typed `AppError` symbols or domain keys.
     * Numbers and dates must use locale-aware platform formatters (`CompactDecimalFormat` on Android, `.formatted()` on Apple).
   * **`GEMINI.md`**: Index the new `localization` skill in its reference list.

---

## Unified Translation Key Catalog (English Baseline)

The following structured keys form the canonical namespace across Android XML and Apple String Catalogs:

### App & Navigation
* `app_name`: `"Image Feed"`
* `nav_tab_photos`: `"Photos"`
* `nav_tab_collections`: `"Collections"`
* `nav_tab_search`: `"Search"`
* `nav_back`: `"Back"`
* `nav_search_accessibility`: `"Search"`
* `nav_shuffle_accessibility`: `"Shuffle"`
* `macos_sidebar_prompt`: `"Select a category from the sidebar"`

### Feed Screen
* `feed_title`: `"Feed"`
* `feed_editorial_tab`: `"Editorial"`
* `feed_randomize_accessibility`: `"Randomize"`
* `feed_user_profile_accessibility`: `"User profile"`
* `feed_shake_loading`: `"🎲 Shaking up a random photo..."`
* `feed_error_title`: `"Error Loading Feed"`
* `feed_error_unknown`: `"Unknown error"`
* `feed_action_retry`: `"Retry"`

### Curated Collections Feed
* `collections_title`: `"CURATED COLLECTIONS"`
* `collections_curated_by`: `"Curated by"`
* `collections_photos_count` (Plural): `"%1$d Photo"` / `"%1$d Photos"`
* `collections_preview_1_accessibility`: `"Preview photo 1"`
* `collections_preview_2_accessibility`: `"Preview photo 2"`
* `collections_curator_avatar_accessibility`: `"Curator profile image"`

### Collection Details
* `collection_detail_title_fallback`: `"Collection"`
* `collection_detail_related`: `"Related Collections"`
* `collection_detail_photos_header`: `"Photos"`

### Search & Discovery
* `search_input_prompt`: `"Search Photos, Collections, Users..."`
* `search_tab_photos`: `"Photos"`
* `search_tab_collections`: `"Collections"`
* `search_tab_users`: `"Users"`
* `search_section_recents`: `"RECENTS"`
* `search_section_popular_topics`: `"POPULAR TOPICS"`
* `search_action_clear_all`: `"CLEAR ALL"`
* `search_action_clear_accessibility`: `"Clear"`
* `search_action_collapse_accessibility`: `"Collapse Search"`
* `search_action_filters_accessibility`: `"Filters"`
* `search_empty_history`: `"No search history"`
* `search_empty_results`: `"No results found."`
* `search_filter_title`: `"FILTERS"`
* `search_filter_action_reset`: `"RESET"`
* `search_filter_action_apply`: `"APPLY FILTERS"`
* `search_filter_sort_by`: `"SORT BY"`
* `search_filter_sort_relevant`: `"RELEVANT"`
* `search_filter_sort_latest`: `"LATEST"`
* `search_filter_orientation`: `"ORIENTATION"`
* `search_filter_orientation_all`: `"ALL"`
* `search_filter_orientation_landscape`: `"LANDSCAPE"`
* `search_filter_orientation_portrait`: `"PORTRAIT"`
* `search_filter_color_tone`: `"COLOR TONE"`
* `search_filter_color_any`: `"Any"`
* `search_filter_color_bw`: `"B&W"`
* `search_filter_color_black`: `"Black"`
* `search_filter_color_white`: `"White"`
* `search_filter_color_yellow`: `"Yellow"`
* `search_filter_color_orange`: `"Orange"`
* `search_filter_color_red`: `"Red"`
* `search_filter_color_purple`: `"Purple"`
* `search_filter_color_magenta`: `"Magenta"`
* `search_filter_color_green`: `"Green"`
* `search_filter_color_teal`: `"Teal"`
* `search_filter_color_blue`: `"Blue"`
* `search_user_card_view_profile_accessibility`: `"View Profile"`

### Photo Details & EXIF Telemetry
* `details_title`: `"Details"`
* `details_action_open_web`: `"Open in Web"`
* `details_action_download`: `"Download High Resolution"`
* `details_action_download_accessibility`: `"Download photo"`
* `details_action_inspect_accessibility`: `"Inspect photo details"`
* `details_error_loading`: `"Failed to load details"`
* `details_placeholder_prompt`: `"Select a photo to view high-resolution imagery, EXIF telemetry, and photographer insights."`
* `details_stat_views`: `"Views"`
* `details_stat_downloads`: `"Downloads"`
* `details_stat_likes`: `"Likes"`
* `details_views_trend_header`: `"HISTORICAL VIEWS (LAST 30 DAYS)"`
* `details_trend_30_day`: `"30-Day Trend"`
* `details_metric_peak`: `"Peak: %s"`
* `details_metric_min`: `"Min: %s"`
* `details_color_palette_header`: `"COLOR PALETTE"`
* `details_copied_to_clipboard`: `"Copied %s to clipboard"`
* `details_exif_header`: `"CAMERA & LENS SPECS"`
* `details_exif_camera`: `"Camera"`
* `details_exif_aperture`: `"Aperture"`
* `details_exif_exposure`: `"Exposure Time"`
* `details_exif_focal_length`: `"Focal Length"`
* `details_exif_iso`: `"ISO"`
* `details_location_header`: `"LOCATION"`
* `details_location_pin_accessibility`: `"Location Pin"`
* `details_location_open_maps`: `"Open in Google Maps"`
* `details_location_fallback_title`: `"Photo Location"`
* `details_location_unknown`: `"Unknown Coordinates"`
* `details_related_tags_header`: `"RELATED TAGS"`

### User Profile & Insights
* `profile_title_fallback`: `"Profile"`
* `profile_tab_photos`: `"Photos"`
* `profile_tab_likes`: `"Likes"`
* `profile_tab_collections`: `"Collections"`
* `profile_tab_insights`: `"Insights"`
* `profile_empty_photos`: `"No photos found."`
* `profile_empty_collections`: `"No collections found."`
* `profile_insights_error`: `"Failed to load insights."`
* `profile_stat_total_views`: `"Total Views"`
* `profile_stat_total_downloads`: `"Total Downloads"`
* `profile_chart_views_trend`: `"VIEWS TRENDS (LAST 30 DAYS)"`
* `profile_chart_downloads_trend`: `"DOWNLOADS TRENDS (LAST 30 DAYS)"`
* `profile_chart_drag_prompt`: `"Drag curve to inspect daily stats"`
* `profile_chart_units`: `"%1$d units"`
* `profile_social_instagram_accessibility`: `"Instagram"`
* `profile_social_twitter_accessibility`: `"Twitter"`
* `profile_social_website_label`: `"Website"`
* `profile_social_website_accessibility`: `"Website"`
* `profile_action_open_browser_accessibility`: `"Open Profile in Browser"`

### System Errors
* `error_network`: `"No internet connection. Please check your network."`
* `error_timeout`: `"The server took too long to respond. Please try again."`
* `error_not_found`: `"The requested item could not be found."`
* `error_server`: `"Server error (%1$d). Please try again later."`
* `error_random_photo`: `"Failed to load random photo. Please try again."`
* `error_unknown`: `"An unexpected error occurred."`

---

## Architectural Invariants & Code Hygiene

1. **Dual-Platform Build Policy**: Whenever changes touch shared or platform layers, verify both Android (`./gradlew :androidApp:assembleDebug`) and iOS (`xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`).
2. **Zero Compiler Warning Policy**: Clean builds with no unresolved warnings or deprecations.
3. **No Dynamic or Reflection DI**: Metro compile-time dependency injection remains unchanged.
4. **API Level Safety**: Strictly enforce API 29 compatibility on Android by utilizing ICU `CompactDecimalFormat` rather than API 34 `java.text.CompactNumberFormat`.
5. **Zero Hardcoded Strings & Agent Instruction Governance**: All user-facing text, accessibility descriptions, and error notifications must be extracted to platform catalogs (`strings.xml`/`plurals.xml` or `Localizable.xcstrings`). ViewModels emit typed `AppError` values. Future development will be governed by `.agents/skills/localization/SKILL.md` and `AGENTS.md`.

---

## Acceptance Criteria

- [x] `AppError` sealed class created in `:core:model` and central `toAppError()` mapper implemented in `:core:network`.
- [x] All 7 KMP feature ViewModels refactored to emit `AppError` in state and effects without constructing English fallback strings.
- [x] `SearchState.kt` default `searchHistory` purged of hardcoded English words.
- [x] All shared `commonTest` suites updated to assert typed `AppError` instances, with 100% test pass rate (`./gradlew allTests`).
- [x] `androidApp/src/main/res/values/strings.xml` and `plurals.xml` populated with all 85 unified keys in English.
- [x] Android screens updated to use `stringResource()` and `pluralStringResource()` with zero hardcoded UI strings.
- [x] Android number formatting updated to ICU `CompactDecimalFormat` and timeline dates updated to `java.time.LocalDate`.
- [x] `iosApp/project.yml` configured with `developmentLanguage: en` and `knownRegions: [en]`.
- [x] `Localizable.xcstrings` created in `iosApp/iosApp/` containing all 85 keys and plural variations.
- [x] SwiftUI views refactored to resolve localized keys with zero hardcoded English text.
- [x] Swift number formatting updated to `.formatted(.number.notation(.compactName))` and timeline dates to `ISO8601DateFormatter`.
- [x] Back buttons on iOS/macOS updated from `chevron.left` to `chevron.backward`.
- [x] `.agents/skills/localization/SKILL.md` authored with complete runbook for string catalogs, format specifiers, plural variations, and error mapping.
- [x] `AGENTS.md` and `GEMINI.md` updated to index the localization skill and enforce the Zero Hardcoded Strings Policy while strictly maintaining `AGENTS.md` under 75 lines.
- [x] Android and iOS builds compile cleanly without warnings or errors.
