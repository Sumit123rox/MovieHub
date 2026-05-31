# CLAUDE.md — MovieHub

Kotlin Multiplatform OTT streaming app for the Stremio addon ecosystem.
Targets: **Android** (JVM 11, minSdk 26, compileSdk 36) + **iOS** (iosArm64, iosSimulatorArm64).

## Build

```bash
./gradlew :composeApp:assembleDebug          # Android debug APK
./gradlew :composeApp:iosSimulatorArm64MainKotlinNativeCompile  # iOS — compile shared Kotlin, then open iosApp/ in Xcode
```

- Kotlin **2.3.21**, Compose Multiplatform **1.11.0**, AGP **9.2.1**, KSP **2.3.9**
- Gradle configuration cache ON, parallel builds ON, `JAVA_HOME` = JDK 17+
- Version catalog: `gradle/libs.versions.toml` (single source of truth for all deps)
- JVM target: 11 (Android), static framework for iOS (`baseName = "ComposeApp"`)

## Module Structure (17 modules)

```
composeApp/         # App entry: App.kt, MainActivity (Android), MainViewController (iOS)
├── core/
│   ├── model/      # Domain models (pure Kotlin, zero deps on other modules)
│   ├── network/    # Ktor client, Stremio API, AddonManager, ScraperManager,
│   │               #   PluginRuntime (QuickJS sandbox), TMDB, YouTube resolver
│   ├── database/   # Room3 KMP — 9 entities, version 11, exportSchema=true
│   ├── ui/         # Shared design system — theme engine, components, composeResources
│   ├── utils/      # PerformanceMonitor, CrashReporter, AnalyticsTracker, DateTimeUtils
│   └── player-api/ # MoviePlayerController abstraction (new module)
├── navigation/     # Type-safe route Screen sealed interface + RootNavGraph
├── di/             # Koin DI — appModules() wires everything
└── feature/
    ├── home/       # Dynamic catalogs, hero carousel, continue watching
    ├── search/     # Cross-addon search, search history
    ├── details/    # Metadata, cast, streams, trailers, seasons/episodes
    ├── player/     # Video player (ExoPlayer Android / AVPlayer iOS)
    ├── addon/      # Addon install & management
    ├── auth/       # Supabase authentication
    ├── profile/    # Profiles, settings, appearance, library
    └── sync/       # Supabase cloud sync
```

**Dependency rule:** `feature → core` and `composeApp → everything`. Core modules never depend on feature modules. `di` wires them together from the outside.

## Architecture Pattern — MVI

Every feature follows strict MVI. **No UseCases. No contracts/interfaces for repositories unless platform-specific.**

### Required files per feature:
| Layer | File(s) | Rule |
|-------|---------|------|
| State | `XxxState.kt` | `@Immutable data class` with default values. All `val`. |
| Action | `XxxState.kt` | `sealed interface XxxAction` in the same file. |
| ViewModel | `XxxViewModel.kt` | Extends `ViewModel`. `MutableStateFlow` + `asStateFlow()`. Single `fun onAction(action: XxxAction)` entry point. |
| Screen | `XxxScreen.kt` | `@Composable` stateless function. Receives state and `onAction` callback. |
| Repository (if data) | `XxxRepository.kt` + `XxxRepositoryImpl.kt` | Interface + impl. Injected via Koin. |

### ViewModel pattern:
```kotlin
class XxxViewModel(/* deps via constructor */) : ViewModel() {
    private val _state = MutableStateFlow(XxxState())
    val state: StateFlow<XxxState> = _state.asStateFlow()

    fun onAction(action: XxxAction) {
        when (action) {
            is XxxAction.DoSomething -> viewModelScope.launch { /* ... */ }
        }
    }
}
```

- State mutations use `_state.value = _state.value.copy(...)` (data class copy).
- Long operations in `viewModelScope.launch`. Use `supervisorScope` for parallel fan-out.
- Repository interfaces live in `feature/<name>/data/`, implementations in same package.

## DI (Koin)

- Single `Koin.kt` file at `di/src/commonMain/kotlin/com/moviehub/di/Koin.kt`.
- `appModules()` returns `List<Module>` — one module per feature + shared modules.
- `viewModel { XxxViewModel(get(), ...) }` for each ViewModel (Koin Compose ViewModel DSL).
- `single<Interface> { Impl(get(), ...) }` for repositories.
- `expect val platformModule: Module` — platform-specific bindings (Android/iOS).
- `appJson` is a shared `Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }` singleton.

## Theme System

- **5 themes:** Nuvio Dark (default), Dark, AMOLED Dark, Light, Ocean Dark.
- **10 accent colors:** Blue (default), Red, Purple, Green, Orange, Pink, Teal, Yellow, Cyan, Rose.
- `MovieHubTheme(themeType: ThemeType, accentType: AccentType)` — reads from `UserPreferencesEntity`.
- **Hard rule:** Use `MaterialTheme.colorScheme.primary` (etc.) for ALL colors. Never hardcode `Color.White`, `Color.Black`, or any literal color in UI composables.
- **Hard rule:** Use `stringResource()` for ALL user-visible strings. Resources live in `composeResources/values/strings.xml`.

## Shared UI Components (`core:ui`)

All reusable components must go here: `ContentCard`, `Poster`, `GlassyBox`, `TechnicalBadge`, `HeroCarousel`, `VerticalGrid`, `Shimmer`, `EmptyState`, `SectionHeader`, `SettingsRow`, `MovieHubTopBar`, `SmartStatusBar`.

## Database (Room3 KMP)

9 entities, version **11**, `fallbackToDestructiveMigration = true` (safe for dev; designed to be replaced before production).
Schema JSONs at `core/database/schemas/` — **committed to git** (Room's `exportSchema = true`).
Cached responses have TTL-based eviction: catalogs 10min, metadata 1hr, streams 20min (configurable per endpoint in `CacheService`).

## Network

- **Ktor 3.5.0** — `HttpClient` with `ContentNegotiation` (JSON), `HttpRequestRetry` (2 server + 3 exception retries, exponential backoff 500ms–8s), `ContentEncoding` (gzip+deflate), `HttpTimeout` (connect 8s, request 20s, socket 15s).
- **Stremio API** — `StremioApiClient` wraps all Stremio protocol endpoints.
- **Addons** — `AddonManager` handles manifest installation and lifecycle. `PluginRepository` + `PluginRuntime` (QuickJS sandbox, 60s timeout) for JS plugins.
- **TMDB** — `TmdbService` + `TmdbEnrichmentService` for optional metadata enrichment (needs user-provided API key).
- **Real-Debrid** — `RealDebridClient` + `HybridStreamResolver` for stream resolution.
- **Supabase** — Auth + cloud sync (URL/key configured in `Koin.kt`).

## Navigation

- `Screen` is a `@Serializable sealed interface` in `navigation/`.
- Routes: `Home`, `Search`, `Addon`, `Auth`, `Profile`, `Settings`, `Details(id, type, addonUrl)`, `PersonDetail(personId, personName)`, `Player(launchId)`, `Appearance`, `Catalog(title, type, catalogId, addonId)`, `Streams(id, type, mediaId)`, `Downloads`, `Library`, `Sync`.
- `RootNavGraph.kt` composes the `NavHost` with all destinations.

## Code Quality

- **Lefthook pre-commit:** Runs `spotlessApply` (ktlint 1.2.1, auto-format) + `detekt` (static analysis) in parallel.
- **Detekt** config at `detekt.yml` — LongMethod threshold 120, LongParameterList threshold 10, LargeClass threshold 600, ComplexMethod threshold 20.
- **Spotless** currently enforced only on `:core:player-api` and `:feature:player` (see root `build.gradle.kts`). Ktlint rules: `target("**/*.kt")`, `trimTrailingWhitespace()`, `endWithNewline()`.
- Wildcard imports allowed (common in KMP Compose `graphics`/`layouts` packages).

## Android-Specific

- `MainActivity` enables edge-to-edge, requests 120Hz refresh rate.
- `StrictMode` — disk reads permitted during startup (Koin/Ktor init), then full detection in debug builds.
- `BaselineProfile` at `composeApp/src/androidMain/baseline-prof.txt`.
- MultiDex enabled. R8 full mode for release. ProGuard at `proguard-rules.pro`.
- Desugaring with `desugar_jdk_libs` (though currently removed from DEX merging — see comment in `composeApp/build.gradle.kts`).

## iOS-Specific

- `MainViewController` wraps `App()` in `ComposeUIViewController`.
- `CBridge/` contains MPV player bridge files (`MpvPlayerBridge.h/.swift`, `mpv_shim.h`).
- `OrientationLockCoordinator.swift` handles rotation lock for video playback.
- Framework is **static** (`isStatic = true`). CInterop commonization enabled.

## File Naming

- Kotlin files: PascalCase (`HomeViewModel.kt`, `ContentCard.kt`).
- Package: `com.moviehub.<module>` for core/feature, `com.sumit.moviehub` for composeApp entry.
- Platform-specific: `Xxx.android.kt`, `Xxx.ios.kt` (KMP expect/actual convention).

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
