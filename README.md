# MovieHub

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.11.0-27C2A0.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-6C63FF.svg)](#)
[![CI](https://github.com/Sumit123rox/MovieHub/actions/workflows/ci.yml/badge.svg)](https://github.com/Sumit123rox/MovieHub/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A **Kotlin Multiplatform** media hub for the Stremio addon ecosystem — browse catalogs, discover metadata, and play streams from **your own addons**. No built-in content, no bundled scrapers. You choose the sources.

> MovieHub is a neutral, privacy-oriented client for the Stremio protocol. All content, metadata, and streams come exclusively from **user-installed third-party addons and JS plugins**.

---

## Features

### 🔍 Discovery
- **Dynamic Home Screen** — Hero carousel with auto-scroll, continue watching with progress bars, category filter chips with emoji labels, shimmer skeleton loading, pull-to-refresh, infinite scroll pagination
- **Multi-Addon Catalogs** — Items from multiple addons are round-robin interleaved within each catalog section for fair source visibility
- **Cache-First Loading** — Room DB cache displayed instantly, network refresh runs in background. Background catalog prefetch while profile screen shows — instant HomeScreen on login
- **Powerful Search** — Cross-addon search with debounced suggestions, persistent search history, discover section with genre/type/sort filters

### 🎬 Player — Buttery Smooth
- **Cross-Platform Video** — ExoPlayer (Android) + **MPV** (iOS) with consistent custom overlay controls
- **Silent Auto-Switch** — Stream fails → next source loads automatically. No error dialogs, no user interaction. Cycles through all available sources. Player auto-closes only when every source is exhausted
- **YouTube-Style Swipe Seek** — Drag anywhere on screen for real-time ±MM:SS seeking with centered indicator. Seek executes on finger lift. 30 seconds per full-screen swipe
- **Pinch Zoom** — Centroid-based zoom (1x–5x) with pan when zoomed in. `graphicsLayer(clip = false)` for smooth overflow rendering
- **Full Controls** — Play/Pause, speed (0.5x–2x), audio track, subtitle track, video quality, brightness/volume gesture zones, screen lock, sleep timer, debug overlay, PiP
- **Proxy Stream Support** — Preconnect with proxy headers, two-hop DNS/TCP/TLS warmup (extractor → CDN), HTTP/2 preferred, 30s read timeout for extractor chains
- **ExoPlayer Tuning** — 1.5s initial buffer (was 50s), 2s rebuffer recovery, OkHttp 10-conn pool, time-prioritized load control
- **Widescreen Subtitle Styling Center** — Real-time WYSIWYG preview, typography controls, color palette, outline/shadow effects, 5 built-in presets (Netflix, Prime, Disney+, High Contrast), Room DB persistence
- **Watch Progress** — Auto-saves every 15s, on pause, and at 90% completion (marks watched). Resume with saved position and track preferences (audio/subtitle/video quality)
- **DRM** — Widevine/ClearKey via Media3 on Android

### 🎨 UI & Personalization
- **5 Themes × 10 Accents** — Nuvio Dark (default), Dark, AMOLED Dark, Light, Ocean Dark × 10 accent colors. Theme-aware status bar across all screens
- **Design System** — All colors via `MaterialTheme.colorScheme.*`, all dimensions via `MovieHubDimens.*`, string resources for localization-ready text
- **Smart TopAppBar** — Consistent across all screens with automatic back navigation
- **Multi-Profile** — Create, clone, and switch profiles. Addons, favorites, watch history, and settings isolated per profile. Cinemeta auto-seeded on new profiles
- **Offline Downloads** — Queue, pause, resume, cancel per profile. Configurable concurrent downloads and storage location

### 🌐 Stream Resolution
- **Parallel Fan-Out** — All 95+ HTTP addons and JS scrapers queried simultaneously via `supervisorScope` + `async/awaitAll` with semaphore-controlled concurrency
- **Real-Time Status Pills** — Per-addon loading/completion/failure indicators. Auto-dismiss when all providers complete
- **Direct-Play + Torrent** — Direct URLs skip validation, torrent streams pass through content verification (title, season/episode, year)
- **Offline-First** — Cached streams displayed instantly. Network refresh merges new results without flicker
- **Provider Filtering** — Filter displayed streams by addon source

### 🏗 Architecture
- **96%+ Common Code** — Business logic, networking, UI, and database are entirely in `commonMain`. Platform code limited to video rendering and file I/O
- **Strict MVI** — Every feature follows `State + Action + ViewModel` with `MutableStateFlow` + `asStateFlow()`. Single `onAction()` entry point. `@Immutable` data classes
- **Clean Module Hierarchy** — `core/*` → `feature/*` → `navigation` → `di` → `composeApp`. Core never depends on feature
- **Offline-First** — Room-backed cache with TTL-based eviction (10min catalogs, 1hr metadata, 20min streams)
- **Reactive** — Koin DI with `StateFlow`-driven ViewModels. Compose recombines on state changes automatically

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | [Kotlin](https://kotlinlang.org/) 2.3.21 — Multiplatform, 96%+ commonMain |
| UI | [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) 1.11.0 — Material 3, shared across Android & iOS |
| Navigation | [Navigation Compose](https://developer.android.com/develop/navigation) 2.9.2 — Type-safe routes with `@Serializable` sealed interfaces |
| DI | [Koin](https://insert-koin.io/) 4.2.1 — Multiplatform DI with compose integration |
| Networking | [Ktor](https://ktor.io/) 3.5.0 — Content negotiation, retry with exponential backoff, gzip/deflate |
| Database | [Room3](https://developer.android.com/kotlin/multiplatform/room) — KMP SQLite ORM, 9 entities, WAL mode |
| Image Loading | [Kamel](https://github.com/Kamel-Media/Kamel) 1.0.9 — Async loading, 50-image memory cache, 50MB disk cache |
| Video (Android) | [Media3 / ExoPlayer](https://developer.android.com/media/media3) 1.10.1 — HLS, DASH, SmoothStreaming, Cast, Widevine |
| Video (iOS) | **MPV** via `mpv-shim` + `MpvPlayerBridge.swift` — Full codec support including MKV, H.265, AV1 |
| JS Runtime | [QuickJS-KT](https://github.com/partouf/quickjs-kt) 1.0.5 — Sandboxed JavaScript, 10s per-plugin timeout, crash protection |
| HTML Parsing | [Ksoup](https://github.com/fleeksoft/ksoup) 0.2.6 — JVM cheerio-compatible HTML parser |
| Backend/Auth | [Supabase](https://supabase.com/) 3.6.0 — PostgREST + Auth SDK |
| Logging | [Kermit](https://github.com/touchlab/Kermit) 2.1.0 — Multiplatform structured logging |
| Serialization | [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) 1.11.0 — JSON with `ignoreUnknownKeys`, `coerceInputValues` |
| Build | AGP 9.2.1 — Gradle configuration cache, parallel builds, KSP 2.3.7 |

---

## Project Structure

```
MovieHub/
├── composeApp/                 # App entry point & Compose root
│   ├── commonMain/             # App.kt, Kamel config
│   ├── androidMain/            # MainActivity (edge-to-edge, 120Hz, PiP)
│   └── iosMain/                # MainViewController (ComposeUIViewController)
│
├── di/                         # Koin DI — appModules() wires everything
├── navigation/                 # RootNavGraph + Screen sealed interface
│
├── core/
│   ├── model/                  # Domain models (pure Kotlin, zero deps)
│   ├── network/                # Ktor client, Stremio API, AddonManager,
│   │                           # ScraperManager + PluginRuntime (QuickJS),
│   │                           # TMDB, YouTube resolver, debrid clients
│   ├── database/               # Room3 — 9 entities, version 11
│   ├── ui/                     # Design system, components, theme engine
│   ├── player-api/             # MoviePlayerController abstraction
│   └── utils/                  # PerformanceMonitor, CrashReporter, Analytics
│
├── feature/
│   ├── home/                   # Hero carousel, catalogs, continue watching
│   ├── search/                 # Cross-addon search, history, discover
│   ├── details/                # Metadata, cast, streams, trailers, series
│   ├── player/                 # Video player (ExoPlayer / MPV)
│   ├── addon/                  # Addon install, update check, management
│   ├── auth/                   # Supabase + debrid authentication
│   ├── profile/                # Profiles, settings, appearance, library
│   └── sync/                   # Cloud sync via Supabase
│
├── gradle/
│   └── libs.versions.toml      # Centralized version catalog (120+ deps)
│
├── iosApp/                     # iOS Xcode project + CBridge (MPV)
│
└── graphify-out/               # Knowledge graph (code relationships)
```

---

## Getting Started

### Prerequisites

- **Android Studio** Koala+ or IntelliJ IDEA with KMP plugin
- **JDK 17+**
- **Xcode 15+** (for iOS)
- A **Stremio addon manifest URL** to get started

### Build

```bash
git clone https://github.com/Sumit123rox/MovieHub.git
cd MovieHub

# Android debug APK
./gradlew :composeApp:assembleDebug

# iOS — compile shared Kotlin, then open in Xcode
./gradlew :composeApp:iosSimulatorArm64MainKotlinNativeCompile
open iosApp/iosApp.xcodeproj
```

> CI runs automatically on push/PR to `main` — see [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

### First Run

1. Launch the app → create a profile
2. Open **External Providers** → paste a Stremio addon URL → Install
3. Home tab shows catalogs from your addons
4. Tap any title → pick a stream → play

> **MovieHub ships with zero built-in content.** You must install addons to see anything.

### Optional

- **TMDB API Key** — Settings → Advanced for enriched metadata
- **Real-Debrid / AllDebrid / Premiumize** — Settings → Sync & Accounts
- **Trakt** — Settings → Sync & Accounts for watchlist sync
- **Appearance** — Settings → General → Theme & Accent (5×10 combinations)

---

## Player Architecture

| Aspect | Android | iOS |
|--------|---------|-----|
| Engine | ExoPlayer (Media3) | **MPV** (libmpv via mpv-shim) |
| Rendering | `AndroidView` + `PlayerView` | `UIKitView` + `MpvPlayerBridge` |
| Position | 1s polling via `Player.Listener` | Frame callback via `MpvPlayerBridge` |
| Formats | HLS, DASH, SmoothStreaming, progressive | All MPV formats — MKV, H.265, AV1, VP9, HLS, DASH |
| DRM | Widevine / ClearKey via Media3 | N/A |
| Cast | Google Cast (Chromecast) | N/A |
| Subtitle Styling | Media3 `CaptionStyleCompat` | MPV's libass renderer |
| Data Source | OkHttp with proxy headers + HTTP/2 | MPV's internal HTTP client |
| Orientation | `requestedOrientation` + restore on exit | `OrientationLockCoordinator.swift` |

---

## Memory & Performance

| Optimization | Detail |
|-------------|--------|
| **Image Cache** | Kamel 50 decoded bitmaps in memory (~25MB), 50MB disk cache |
| **Stream Cap** | Bounded accumulation, early exit when sufficient streams found |
| **Concurrency** | Semaphore-controlled parallel addon queries |
| **Recomposition** | `StreamsUpdated` throttled to 250ms, `pointerInput(Unit)` stable keys |
| **ExoPlayer** | 1.5s initial buffer, 2s rebuffer, time-prioritized load control |
| **Heap** | `largeHeap=true` for headroom on large video buffers |

---

## Contributing

PRs and ideas welcome — open an issue first to discuss.

## License

MIT — see [LICENSE](LICENSE).

---

<p align="center">
Made with ❤️ in India<br>
<b>⌂ MovieHub</b>
</p>
