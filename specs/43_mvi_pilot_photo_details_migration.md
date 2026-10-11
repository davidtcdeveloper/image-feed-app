# Specification 43: MVI Pilot Migration - Photo Details

**Status:** Approved

## 1. Overview & Objectives

Following the foundation established in [Spec 42](file:///Users/davidtiagoconceicao/Developer/image-feed-app/specs/42_mvi_foundation_and_transition_guidelines.md), this specification migrates the **Photo Details** feature from [`PhotoDetailsPresenter.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/presentation/PhotoDetailsPresenter.kt) to `PhotoDetailsViewModel` (`MviViewModel`). 

Photo Details is the optimal pilot candidate because:
1. It is a self-contained detail screen driven by a dynamic parameter (`photoId`).
2. It exercises **Metro `@AssistedFactory` injection**.
3. It exercises Android Compose navigation back-stack scoping via **Jetpack Navigation 3**.
4. It exercises Apple SwiftUI observation and sensory triggers (e.g. haptic feedback on save) via **`@Observable` and one-off effects**.
5. It has an existing comprehensive unit test suite in [`PhotoDetailsPresenterTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/PhotoDetailsPresenterTest.kt).

---

## 2. Technical Contracts & Implementation Details

### A. Shared MVI Contract (`shared/src/commonMain/kotlin/com/example/imagefeed/presentation/details/`)
1. **State**:
   ```kotlin
   data class PhotoDetailsState(
       val photo: Photo? = null,
       val isLoading: Boolean = false,
       val error: String? = null,
   ) : UiState
   ```
2. **Intent**:
   ```kotlin
   sealed interface PhotoDetailsIntent : UiIntent {
       data object LoadDetails : PhotoDetailsIntent
       data class TrackDownload(val photo: Photo) : PhotoDetailsIntent
       data object Retry : PhotoDetailsIntent
   }
   ```
3. **Effect**:
   ```kotlin
   sealed interface PhotoDetailsEffect : UiEffect {
       data object DismissScreen : PhotoDetailsEffect
       data class ShowErrorSnackbar(val message: String) : PhotoDetailsEffect
   }
   ```
4. **ViewModel (`PhotoDetailsViewModel`)**:
   - Subclasses `MviViewModel<PhotoDetailsState, PhotoDetailsIntent, PhotoDetailsEffect>`.
   - Injected with `UnsplashRepository` and `@Assisted private val photoId: String`.
   - Uses `viewModelScope` for background fetching; no manual `PresenterScopeFactory`.

### B. Metro DI Assisted Factory
```kotlin
@Inject
class PhotoDetailsViewModel(
    private val repository: UnsplashRepository,
    @Assisted private val photoId: String,
) : MviViewModel<PhotoDetailsState, PhotoDetailsIntent, PhotoDetailsEffect>(PhotoDetailsState()) {

    @AssistedFactory
    interface Factory {
        fun create(photoId: String): PhotoDetailsViewModel
    }
    // ...
}
```
Expose `photoDetailsViewModelFactory: PhotoDetailsViewModel.Factory` on [`ApplicationGraph`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonMain/kotlin/com/example/imagefeed/di/Metro.kt) and `getPhotoDetailsViewModel(photoId: String)` on `MetroHelper`.

### C. Android Presentation Shell (`androidApp`)
- Update `PhotoDetailsScreen.kt`:
  - Receive `viewModel: PhotoDetailsViewModel`.
  - Collect state using `val state by viewModel.state.collectAsStateWithLifecycle()`.
  - Collect effects using `LaunchedEffect(viewModel)` to display snackbars or navigate back.
  - Dispatch user interactions via `viewModel.dispatch(PhotoDetailsIntent...)`.

### D. Apple Presentation Shell (`iosApp` & `macosApp`)
- Update [`PhotoDetailsViewModel.swift`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/iosApp/iosApp/PhotoDetailsViewModel.swift):
  - Retain `viewModel: PhotoDetailsViewModel` instantiated via `MetroHelper.shared.getPhotoDetailsViewModel(photoId: photoId)`.
  - Watch `viewModel.iosState` and `viewModel.iosEffects`.
  - Expose `func send(_ intent: PhotoDetailsIntent)` or typed helper functions.
  - Invoke `viewModel.clear()` inside `deinit`.

### E. Unit Testing (`shared/commonTest`)
- Replace [`PhotoDetailsPresenterTest.kt`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/shared/src/commonTest/kotlin/com/example/imagefeed/presentation/PhotoDetailsPresenterTest.kt) with `PhotoDetailsViewModelTest.kt`.
- Use standard `StandardTestDispatcher` with `runTest`:
  - Assert state transitions when `dispatch(PhotoDetailsIntent.LoadDetails)` is sent.
  - Assert effect emissions when errors occur.

---

## 3. Implementation Steps

- [ ] **Step 1: Implement `PhotoDetailsViewModel` & Contracts**
  - Define `PhotoDetailsState`, `PhotoDetailsIntent`, `PhotoDetailsEffect`.
  - Implement `PhotoDetailsViewModel` extending `MviViewModel`.
  - Configure Metro `@AssistedFactory`.

- [ ] **Step 2: Update Metro DI Graph**
  - Add `photoDetailsViewModelFactory` to `ApplicationGraph`.
  - Add helper function `MetroHelper.getPhotoDetailsViewModel(photoId)`.

- [ ] **Step 3: Update Android Compose Screen**
  - Adapt `PhotoDetailsScreen.kt` and Navigation 3 entry scoping to use `PhotoDetailsViewModel`.

- [ ] **Step 4: Update Apple SwiftUI Shell**
  - Adapt `PhotoDetailsViewModel.swift` and `PhotoDetailsView.swift` to consume state and dispatch intents.

- [ ] **Step 5: Migrate Unit Tests**
  - Implement `PhotoDetailsViewModelTest.kt` verifying UDF flow and effect channels.

- [ ] **Step 6: Dual-Platform Verification**
  - Run `./gradlew :shared:testDebugUnitTest`.
  - Build Android: `./gradlew :androidApp:assembleDebug`.
  - Build iOS: `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`.

---

## 4. Acceptance Criteria

- [ ] `PhotoDetailsViewModel` completely replaces `PhotoDetailsPresenter` in production code.
- [ ] Photo Details feature behaves identically across Android Compose and iOS/macOS SwiftUI.
- [ ] Metro DI compiles cleanly without runtime reflection.
- [ ] Unit tests pass with 100% coverage of state transitions and intents.
- [ ] Dual-platform builds pass with zero compiler warnings.
