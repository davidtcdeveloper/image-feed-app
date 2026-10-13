# Specification 44: MVI Core Features Migration

**Status:** Implemented

## 1. Overview & Objectives

Following the successful pilot validation in [Spec 43](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/43_mvi_pilot_photo_details_migration.md), this specification details the systematic migration of all remaining presenters to **`MviViewModel`**:
1. **Collection Detail** ([`CollectionDetailPresenter.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/CollectionDetailPresenter.kt))
2. **Collections Feed** ([`CollectionsFeedPresenter.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/CollectionsFeedPresenter.kt))
3. **User Profile** ([`UserProfilePresenter.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/UserProfilePresenter.kt))
4. **Random Photo** ([`RandomPhotoPresenter.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/RandomPhotoPresenter.kt))
5. **Feed** ([`FeedPresenter.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/FeedPresenter.kt))
6. **Unified Search** ([`UnifiedSearchPresenter.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/UnifiedSearchPresenter.kt))

---

## 2. Feature-by-Feature Contract Specifications

### 2.1 Collection Detail
- **Target**: `CollectionDetailViewModel : MviViewModel<CollectionDetailState, CollectionDetailIntent, CollectionDetailEffect>`
- **Intents**:
  - `LoadCollection`, `LoadNextPage`, `Refresh`, `TrackDownload(photo: Photo)`
- **Effects**:
  - `NavigateBack`, `ShowError(message: String)`
- **DI**: Metro `@AssistedFactory` with `@Assisted val collectionId: String`.

### 2.2 Collections Feed
- **Target**: `CollectionsFeedViewModel : MviViewModel<CollectionsFeedState, CollectionsFeedIntent, CollectionsFeedEffect>`
- **Intents**:
  - `LoadInitial`, `LoadNextPage`, `Refresh`
- **Effects**:
  - `ScrollToTop`, `ShowError(message: String)`
- **DI**: Metro `@Inject constructor(...)`.

### 2.3 User Profile
- **Target**: `UserProfileViewModel : MviViewModel<UserProfileState, UserProfileIntent, UserProfileEffect>`
- **Intents**:
  - `LoadProfile`, `SelectTab(tab: ProfileTab)`, `LoadNextPhotosPage`, `LoadNextLikesPage`, `LoadNextCollectionsPage`
- **Effects**:
  - `NavigateBack`, `ShowError(message: String)`
- **DI**: Metro `@AssistedFactory` with `@Assisted val username: String`.

### 2.4 Random Photo
- **Target**: `RandomPhotoViewModel : MviViewModel<RandomPhotoState, RandomPhotoIntent, RandomPhotoEffect>`
- **Intents**:
  - `FetchRandomPhoto`
- **Effects**:
  - `PhotoFetched(photoId: String)`, `FetchFailed(error: String)`
- **DI**: Metro `@Inject constructor(...)`.

### 2.5 Main Feed
- **Target**: `FeedViewModel : MviViewModel<FeedState, FeedIntent, FeedEffect>`
- **Intents**:
  - `LoadTopics`, `SelectTopic(slug: String)`, `LoadNextPage`, `Refresh`, `TrackDownload(photo: Photo)`
- **Effects**:
  - `ScrollToTop`, `ShowToast(message: String)`
- **DI**: Metro `@Inject constructor(...)`.

### 2.6 Unified Search
- **Target**: `UnifiedSearchViewModel : MviViewModel<UnifiedSearchState, UnifiedSearchIntent, UnifiedSearchEffect>`
- **Intents**:
  - `UpdateQuery(query: String)` (with debouncing in `viewModelScope`)
  - `SelectSearchType(type: SearchType)`
  - `SelectColor(color: String?)`
  - `SelectOrientation(orientation: String?)`
  - `LoadNextPage`
  - `ClearSearch`
- **Effects**:
  - `DismissKeyboard`, `ScrollToTop`, `ShowError(message: String)`
- **DI**: Metro `@Inject constructor(...)`.

---

## 3. Platform View Shell & Test Suite Migration

### A. Android Jetpack Compose Screens (`androidApp`)
- Replace presenter parameter references with `viewModel` in:
  - `FeedScreen.kt`
  - `CollectionsFeedScreen.kt`
  - `CollectionDetailScreen.kt`
  - `SearchScreen.kt`
  - `UserProfileScreen.kt`
- Bind interactions to `viewModel.dispatch(intent)` and collect effects via `LaunchedEffect`.

### B. Apple SwiftUI ViewModels (`iosApp` & `macosApp`)
- Update Swift state holders to hold `MviViewModel`:
  - `FeedViewModel.swift`
  - `CollectionsFeedViewModel.swift`
  - `CollectionDetailViewModel.swift`
  - `SearchViewModel.swift`
  - `UserProfileViewModel.swift`
- Route view actions into `viewModel.dispatch(...)` and handle effects on `@MainActor`.

### C. Shared Unit Tests (`shared/commonTest`)
- Migrate existing test suites:
  - `CollectionDetailPresenterTest.kt` -> `CollectionDetailViewModelTest.kt`
  - `FeedPresenterTest.kt` -> `FeedViewModelTest.kt`
  - `UnifiedSearchPresenterTest.kt` -> `UnifiedSearchViewModelTest.kt`
- Add tests for `UserProfileViewModelTest` and `RandomPhotoViewModelTest`.

---

## 4. Implementation Steps

- [x] **Step 1: Migrate Collection Detail & Collections Feed**
- [x] **Step 2: Migrate User Profile & Random Photo**
- [x] **Step 3: Migrate Main Feed**
- [x] **Step 4: Migrate Unified Search (with Debounced Intents)**
- [x] **Step 5: Migrate All Unit Test Suites to Coroutine Dispatchers**
- [x] **Step 6: Dual-Platform Verification**
  - Verify Android assemble and unit tests.
  - Verify iOS xcodebuild and SwiftUI view shells.

---

## 5. Acceptance Criteria

- [x] All 6 core features are powered by `MviViewModel`.
- [x] All user interactions flow through typed, sealed `Intent` pipelines.
- [x] All one-off events are delivered via decoupled `Effect` flows.
- [x] Zero presenter calls remain in Android and Apple UI shells.
- [x] Dual-platform builds pass with zero compiler warnings.
