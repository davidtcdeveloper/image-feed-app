# Specification 48: Modularization Phase 3 - Remaining Feature Extractions (`:feature:feed`, `:feature:search`, `:feature:collections`, `:feature:profile`)

**Status:** Approved

## 1. Overview & Problem Statement

Building upon the foundation core extraction ([Spec 46](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/46_modularization_foundation_core_extraction.md)) and the successful pilot migration of Photo Details ([Spec 47](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/47_modularization_pilot_feature_details.md)), **Phase 3** extracts all remaining business domains into vertical feature modules (**Option B: Vertical Feature Ownership**):
- [`:feature:feed`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/researches/modularization_evaluation.md#L553-L557)
- [`:feature:search`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/researches/modularization_evaluation.md#L558-L562)
- [`:feature:collections`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/researches/modularization_evaluation.md#L563-L567)
- [`:feature:profile`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/researches/modularization_evaluation.md#L568-L573)

Under Option B, each feature module owns its complete vertical stack: MVI presentation, dedicated domain repository, internal repository implementation, internal API client, feature-specific domain models (such as `UserStats` in `:feature:profile`), and concise test fakes.

Crucially, **Phase 3 completely retires and deletes the legacy monolithic [`UnsplashRepository`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/repository/UnsplashRepository.kt) and [`UnsplashApiClient`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/api/UnsplashApiClient.kt)** from `:shared`. All 17-method "god fakes" in test files are permanently replaced with lean, feature-specific fakes. Download tracking across all features delegates directly to `PhotoDownloadTracker` from `:core:network`.

Upon completion of Phase 3, zero presentation logic, data access classes, or domain ViewModels will reside in `:shared`. `:shared` transforms purely into an umbrella framework and compile-time Metro DI aggregator.

---

## 2. Technical Architecture & Module Breakdown

```mermaid
graph TD
    subgraph UI Shells
        Android[androidApp]
        Apple[iosApp / macosApp]
    end

    subgraph Assembly & Umbrella
        Shared[":shared (Umbrella Framework & Metro Root Graph)"]
    end

    subgraph Feature Modules [Vertical Feature Modules (VM + Repo + Client + Fakes)]
        FeatFeed[":feature:feed\n(FeedVM, RandomVM, FeedRepo, FeedApiClient)"]
        FeatDetails[":feature:details\n(PhotoDetailsVM, DetailsRepo, DetailsApiClient, PhotoStats)"]
        FeatSearch[":feature:search\n(SearchVM, SearchRepo, SearchApiClient)"]
        FeatCollections[":feature:collections\n(CollectionsVMs, CollectionsRepo, CollectionsApiClient)"]
        FeatProfile[":feature:profile\n(ProfileVM, ProfileRepo, ProfileApiClient, UserStats)"]
    end

    subgraph Core Modules [Foundational Core Modules]
        CoreNetwork[":core:network\n(Ktor Engine, PhotoDownloadTracker, CollectionsExt)"]
        CoreMvi[":core:mvi\n(MviViewModel, UiState, UiIntent, UiEffect, CommonFlow)"]
        CoreModel[":core:model\n(Universal Entities: Photo, Topic, Collection, User, SearchResponse)"]
    end

    Android --> Shared
    Apple --> Shared

    Shared --> FeatFeed
    Shared --> FeatDetails
    Shared --> FeatSearch
    Shared --> FeatCollections
    Shared --> FeatProfile

    FeatFeed --> CoreNetwork
    FeatFeed --> CoreMvi
    FeatFeed --> CoreModel

    FeatSearch --> CoreNetwork
    FeatSearch --> CoreMvi
    FeatSearch --> CoreModel

    FeatCollections --> CoreNetwork
    FeatCollections --> CoreMvi
    FeatCollections --> CoreModel

    FeatProfile --> CoreNetwork
    FeatProfile --> CoreMvi
    FeatProfile --> CoreModel
```

### A. Feature Module Specifications

#### 1. `:feature:feed`
* **Path**: `feature/feed`
* **Contents**:
  - Presentation:
    - [`FeedViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/feed/FeedViewModel.kt), `FeedState.kt`, `FeedIntent.kt`, `FeedEffect.kt`
    - [`RandomPhotoViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModel.kt), `RandomPhotoState.kt`, `RandomPhotoIntent.kt`, `RandomPhotoEffect.kt`
  - Domain / Data:
    - `FeedRepository`: contract declaring `getPhotos`, `getRandomPhotos`, `getTopics`, `getTopicPhotos`.
    - `internal class FeedRepositoryImpl(private val apiClient: FeedApiClient)`
    - `internal class FeedApiClient(private val client: HttpClient)` querying `photos`, `photos/random`, `topics`, and `topics/{id}/photos`.
  - Download Tracking: Injects `PhotoDownloadTracker` from `:core:network`.
  - Pagination Helpers: Consumes `filterDistinctAgainst` from `:core:network`.
* **Unit Tests & Fakes**:
  - Migrated from [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/feed/FeedViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/feed/FeedViewModelTest.kt) and [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModelTest.kt) to `feature/feed/src/commonTest/`.
  - `FakeFeedRepository`: Lean fake implementing only feed and topic photo queries.
* **Target Platforms**: `android` (namespace `com.example.imagefeed.feature.feed`), `iosArm64`, `iosSimulatorArm64`, `macosArm64`.
* **DI Wiring**: Standard `@Inject constructor(...)` for both ViewModels.

#### 2. `:feature:search`
* **Path**: `feature/search`
* **Target Platforms**: `android` (namespace `com.example.imagefeed.feature.search`), `iosArm64`, `iosSimulatorArm64`, `macosArm64`.
* **Contents**:
  - Presentation:
    - [`UnifiedSearchViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/search/UnifiedSearchViewModel.kt), `SearchState.kt`, `UnifiedSearchIntent.kt`, `UnifiedSearchEffect.kt`
  - Domain / Data:
    - `SearchRepository`: contract declaring `searchPhotos`, `searchCollections`, and `searchUsers`.
    - `internal class SearchRepositoryImpl(private val apiClient: SearchApiClient)`
    - `internal class SearchApiClient(private val client: HttpClient)` querying `search/photos`, `search/collections`, and `search/users`.
  - Pagination Helpers: Consumes `filterDistinctAgainst` from `:core:network`.
* **Unit Tests & Fakes**:
  - Migrated from [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/search/UnifiedSearchViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/search/UnifiedSearchViewModelTest.kt) to `feature/search/src/commonTest/`.
  - `FakeSearchRepository`: Lean fake implementing only search queries.
* **DI Wiring**: Standard `@Inject constructor(...)`.

#### 3. `:feature:collections`
* **Path**: `feature/collections`
* **Target Platforms**: `android` (namespace `com.example.imagefeed.feature.collections`), `iosArm64`, `iosSimulatorArm64`, `macosArm64`.
* **Contents**:
  - Presentation:
    - [`CollectionsFeedViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/collections/CollectionsFeedViewModel.kt), `CollectionsFeedState.kt`, `CollectionsFeedIntent.kt`, `CollectionsFeedEffect.kt`
    - [`CollectionDetailViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/collection/CollectionDetailViewModel.kt), `CollectionDetailState.kt`, `CollectionDetailIntent.kt`, `CollectionDetailEffect.kt`
  - Domain / Data:
    - `CollectionsRepository`: contract declaring `getCollections`, `getCollection`, `getCollectionPhotos`, and `getRelatedCollections`.
    - `internal class CollectionsRepositoryImpl(private val apiClient: CollectionsApiClient)`
    - `internal class CollectionsApiClient(private val client: HttpClient)` querying `collections`, `collections/{id}`, `collections/{id}/photos`, and `collections/{id}/related`.
  - Download Tracking: Injects `PhotoDownloadTracker` from `:core:network`.
  - Pagination Helpers: Consumes `filterDistinctAgainst` from `:core:network`.
* **Unit Tests & Fakes**:
  - Migrated from [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/collections/CollectionsFeedViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/collections/CollectionsFeedViewModelTest.kt) and [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/collection/CollectionDetailViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/collection/CollectionDetailViewModelTest.kt) to `feature/collections/src/commonTest/`.
  - `FakeCollectionsRepository`: Lean fake implementing only collection queries.
* **DI Wiring**:
  - `CollectionsFeedViewModel`: Standard `@Inject constructor(...)`
  - `CollectionDetailViewModel`: `@AssistedInject` and `@AssistedFactory` (`collectionId: String`)

#### 4. `:feature:profile`
* **Path**: `feature/profile`
* **Target Platforms**: `android` (namespace `com.example.imagefeed.feature.profile`), `iosArm64`, `iosSimulatorArm64`, `macosArm64`.
* **Contents**:
  - Presentation:
    - [`UserProfileViewModel.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/profile/UserProfileViewModel.kt), `UserProfileState.kt`, `UserProfileIntent.kt`, `UserProfileEffect.kt`
  - Domain / Data:
    - `ProfileRepository`: contract declaring `getUserProfile`, `getUserPhotos`, `getUserLikes`, `getUserCollections`, and `getUserStats`.
    - `internal class ProfileRepositoryImpl(private val apiClient: ProfileApiClient)`
    - `internal class ProfileApiClient(private val client: HttpClient)` querying `users/{username}`, `users/{username}/photos`, `users/{username}/likes`, `users/{username}/collections`, and `users/{username}/statistics`.
  - Feature Domain Model:
    - Migrate `UserStats` from `shared/.../model/Photo.kt` to `feature/profile/src/commonMain/kotlin/com/example/imagefeed/profile/model/UserStats.kt`.
  - Download Tracking: Injects `PhotoDownloadTracker` from `:core:network`.
* **Unit Tests & Fakes**:
  - Migrated from [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/profile/UserProfileViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/profile/UserProfileViewModelTest.kt) to `feature/profile/src/commonTest/`.
  - `FakeProfileRepository`: Lean fake implementing only profile queries.
* **DI Wiring**: `@AssistedInject` and `@AssistedFactory` (`username: String`)

### B. Retirement of Legacy Monolithic Data Layer & Metro Multi-Module Assembly
Once all four features are extracted:
1. Delete [`UnsplashRepository.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/repository/UnsplashRepository.kt) and [`UnsplashApiClient.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/api/UnsplashApiClient.kt) from `:shared`.
2. Remove legacy repository provider methods from `AppModule` in [`Metro.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt).
3. Each feature module declares a `public` Metro binding container (or uses `@ContributesTo(AppScope::class)`), binding its public repository interface to its `internal class *RepositoryImpl`. This allows the root [`ApplicationGraph`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt) in `:shared` to assemble the multi-module graph cleanly while keeping concrete data implementations encapsulated.

### C. Inter-Feature Decoupling Invariant
Features MUST NOT declare dependencies on other feature modules (e.g. `:feature:feed` has no dependency on `:feature:profile` or `:feature:details`). Navigation routing across domain boundaries is handled entirely by platform navigation containers via typed navigation parameters or effects:
- On **Android**: Navigation 3 backstack events (`navigator.navigate(...)`).
- On **Apple**: SwiftUI typed path elements (`FeedPathItem`, `CollectionPathItem`).

---

## 3. Implementation Steps

- [ ] **Step 1: Module Directory & Build Script Scaffolding**
  - Add `include(":feature:feed")`, `include(":feature:search")`, `include(":feature:collections")`, and `include(":feature:profile")` to `settings.gradle.kts`.
  - Author `build.gradle.kts` for each feature module applying KMP, Android Library, and Metro plugins, configuring respective namespaces (`com.example.imagefeed.feature.*`), with dependencies on `:core:model`, `:core:mvi`, and `:core:network`.
- [ ] **Step 2: Migrate Feed Feature & Fakes**
  - Create `FeedRepository`, `internal class FeedRepositoryImpl`, and `internal class FeedApiClient`.
  - Move `presentation/feed` and `presentation/random` to `feature/feed/`.
  - Move tests from [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/feed/FeedViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/feed/FeedViewModelTest.kt) and [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/random/RandomPhotoViewModelTest.kt) to `feature/feed/src/commonTest`.
  - Author `FakeFeedRepository` replacing monolithic fake.
- [ ] **Step 3: Migrate Search Feature & Fakes**
  - Create `SearchRepository`, `internal class SearchRepositoryImpl`, and `internal class SearchApiClient`.
  - Move `presentation/search` to `feature/search/`.
  - Move tests from [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/search/UnifiedSearchViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/search/UnifiedSearchViewModelTest.kt) to `feature/search/src/commonTest`.
  - Author `FakeSearchRepository` replacing monolithic fake.
- [ ] **Step 4: Migrate Collections Feature & Fakes**
  - Create `CollectionsRepository`, `internal class CollectionsRepositoryImpl`, and `internal class CollectionsApiClient`.
  - Move `presentation/collections` and `presentation/collection` to `feature/collections/`.
  - Move tests from [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/collections/CollectionsFeedViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/collections/CollectionsFeedViewModelTest.kt) and [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/collection/CollectionDetailViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/collection/CollectionDetailViewModelTest.kt) to `feature/collections/src/commonTest`.
  - Author `FakeCollectionsRepository` replacing monolithic fake.
- [ ] **Step 5: Migrate Profile Feature & Fakes**
  - Relocate `UserStats` model from `Photo.kt` to `feature/profile/`.
  - Create `ProfileRepository`, `internal class ProfileRepositoryImpl`, and `internal class ProfileApiClient`.
  - Move `presentation/profile` to `feature/profile/`.
  - Move tests from [`shared/src/commonTest/kotlin/com/example/imagefeed/presentation/profile/UserProfileViewModelTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/profile/UserProfileViewModelTest.kt) to `feature/profile/src/commonTest`.
  - Author `FakeProfileRepository` replacing monolithic fake.
- [ ] **Step 6: Decommission Legacy Data Layer in `:shared`**
  - Delete `UnsplashRepository.kt` and `UnsplashApiClient.kt` from `:shared`.
  - Clean up `AppModule`, `ApplicationGraph`, and `MetroHelper` in `Metro.kt`.
- [ ] **Step 7: Update `:shared` Metro DI Graph and Umbrella Exports**
  - Add `api(projects.feature.*)` dependencies in `shared/build.gradle.kts`.
  - Export all feature modules in the Apple framework binary configuration.
  - Verify [`ApplicationGraph`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt) and [`MetroHelper`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt) resolve all un-parameterized and assisted factories across the multi-module graph.
  - Author `ApplicationGraphTest.kt` in `shared/src/commonTest/` verifying compile-time DI graph assembly and runtime factory resolution.
- [ ] **Step 8: Verification & Test Execution**
  - Run tests across all feature modules and umbrella graph:
    `./gradlew :feature:feed:allTests :feature:search:allTests :feature:collections:allTests :feature:profile:allTests :shared:allTests`
  - Verify dual-platform compilation:
    - Android: `./gradlew :androidApp:assembleDebug`
    - iOS/macOS: `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`
  - Execute linters and formatters (`ktlintFormat`, `ktlintCheck`, `detekt`, `swiftformat .`, `swiftlint`).

---

## 4. Acceptance Criteria

- [ ] All presentation domains are isolated within dedicated vertical feature modules owning their repositories, API clients, and fakes, with unique Android namespaces (`com.example.imagefeed.feature.*`).
- [ ] The monolithic `UnsplashRepository` and `UnsplashApiClient` are fully retired and deleted from `:shared`.
- [ ] All test fakes are lean and feature-specific; all test paths are corrected.
- [ ] Zero ViewModels or presentation state machines remain in the `:shared` module.
- [ ] Features are strictly independent with zero inter-feature dependencies.
- [ ] Root `MetroHelper.graph` instantiation is verified via `ApplicationGraphTest` in `:shared`.
- [ ] Android Navigation 3 and Apple SwiftUI navigation work seamlessly without regressions.
- [ ] All unit tests pass in their respective feature modules and `:shared`.
