# Specification 60: Android Shimmer & Skeleton Rendering Optimization

**Status:** Implemented

## 1. Overview & Problem Statement

Performance audit of `androidApp` (Jetpack Compose performance skill: `animation.md`, `graphics-performance.md`, `state-and-stability.md`) found that skeleton loading placeholders do per-frame work in the **composition** phase instead of the **draw** phase.

### Verified findings

- [`Modifier.shimmerEffect`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/AnimationUtils.kt#L137-L165) is a `@Composable` modifier factory. Each call creates its own `rememberInfiniteTransition`, reads the animated float via `by` delegation in composition, rebuilds the colour list and a `Brush.linearGradient` on every frame, then applies `Modifier.background(brush)`. Every tick therefore invalidates composition of the enclosing scope, not just draw.
- Fan-out is large. [`PhotoCardSkeleton`](file:///Users/davidtiagoconceicao/Developer/image-feed-app/androidApp/src/main/java/com/example/imagefeed/android/util/AnimationUtils.kt#L168) applies 4 shimmers (card, image, avatar, name). `PhotoGridSkeleton` renders 12 cards = **48 independent infinite transitions** recomposing every frame. `CollectionMosaicCardSkeleton` applies 6 per card (3 cards in the profile Collections tab = 18). `UserProfileHeaderSkeleton` applies 5. `shimmerEffect()` has 14 call sites, all inside `util/AnimationUtils.kt` (verified by grep), so the change stays local to that file.
- The card container and its children both shimmer, so the child boxes are painted over an already-shimmering parent (overdraw), and each element's sweep is phase-independent and uses element-local pixel offsets (-1000..1000 px), so sweeps look desynchronised and are density-dependent (px, not dp).
- Skeletons are shown on cold start for Feed, Search, Profile, Collections, so this affects first impression and low-end devices most.

## 2. Technical Design

1. **Single shared animation clock per skeleton screen**: one infinite transition hoisted at the skeleton root (e.g. each `*Skeleton` container) and exposed to children through a composition local or a lambda/`State` holder that is **read only inside draw** (not in composition). Children never create their own transition.
2. **Draw-phase brush**: reimplement `shimmerEffect` as a draw-phase modifier (`drawWithCache` keyed on colours + size, or a `Modifier.Node` implementing `DrawModifierNode`) so animation ticks only invalidate draw. The brush is built from cached colour list; the translation is applied with a lambda read at draw time (e.g. shader matrix / `translate` of a pre-built wide brush) so no `Brush` is allocated per frame.
3. **Window-relative sweep**: derive gradient extent from the skeleton container size (or `LocalWindowInfo`) so all placeholders share one coherent sweep, density-independent (dp -> px at draw).
4. **Remove nested shimmer**: container background is a static `surfaceContainerLow` colour; only leaf placeholders shimmer. Keep the visual appearance equivalent (M3 tokens `surfaceContainerLow`/`surfaceContainerHigh`, see `android-m3-design` skill).
5. **Accessibility**: respect reduced-motion (animator duration scale == 0 -> static placeholder) if feasible without extra dependencies; otherwise record as follow-up.
6. Public API of `PhotoGridSkeleton`, `PhotoCardSkeleton`, `CollectionMosaicCardSkeleton`, `UserProfileHeaderSkeleton` stays unchanged for callers (`FeedScreen`, `UserProfileScreen`, etc.).

## 3. Verification

- Layout Inspector recomposition counts: skeleton composables must show 0 recompositions while idle-animating (before: counted every frame).
- Macro/Perfetto frame timing on Feed cold start (reuse harness from Spec 64 when available).
- Visual diff on Pixel emulator in light/dark and dynamic colour.
- `./gradlew :androidApp:assembleDebug`, `./gradlew :androidApp:lintDebug` (see `verification-and-linting` skill); iOS build not impacted but run the dual-platform build per AGENTS.md if `shared/` is touched (it should not be).

## 4. Acceptance Criteria

- [x] `shimmerEffect` no longer reads animation state in composition and allocates no `Brush`/`List` per frame.
- [x] Only one infinite transition is active per visible skeleton screen.
- [x] No nested shimmering containers (parent static, leaves animated).
- [x] Sweep is density-independent and visually coherent across all placeholders.
- [x] All existing skeleton composables render equivalently; call sites unchanged.
- [x] Zero new compiler warnings; zero dead code (old transition code removed).
- [x] Android debug build and lint pass.

> [!NOTE]
> Line numbers in this spec are approximate (working tree had uncommitted edits at authoring time); locate code by symbol name.
