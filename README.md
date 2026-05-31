# MovieHub

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.11.0-27C2A0.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-6C63FF.svg)](#)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A **Kotlin Multiplatform** media hub for the Stremio addon ecosystem — browse catalogs, discover metadata, and play streams from **your own addons**. No built-in content, no bundled scrapers. You choose the sources.

> MovieHub is a neutral, privacy-oriented client for the Stremio protocol. All content, metadata, and streams come exclusively from **user-installed third-party addons and JS plugins**.

## Features

### 📺 Streaming
- **Stremio Protocol** — Install any Stremio-compatible addon by manifest URL. Browse catalogs, fetch metadata, and discover streams using the standard Stremio API.
- **JavaScript Plugin System** — Sandboxed QuickJS runtime with native bridge for `fetch`, `cheerio` HTML parsing, and `CryptoJS` — all backed by Ktor HTTP, Ksoup, and platform crypto. 60-second execution timeout, sandboxed execution.
- **YouTube Trailer Resolution** — InnerTube API extractor with 3 client profiles (Android VR, Android, TVHTML5). Prefers HLS, falls back to progressive MP4.
- **TMDB Enrichment** — Optional API key unlocks cast photos, recommendations, trailers, age ratings, and production company data on every title.

### 🎬 Player
- **Cross-Platform** — ExoPlayer (Android) + AVPlayer (iOS) with consistent custom overlay controls.
- **Full Controls** — Speed (0.5x–2x), audio track selection, subtitle track selection, video scale (Fit/Fill/Zoom/Stretch), free zoom with pinch gestures, brightness/volume swipe gestures.
- **Widescreen Subtitle Styling Center** — Premium side-by-side 50/50 dashboard with a real-time WYSIWYG preview. Advanced typography (font family, letter-spacing, line-height), colors (circles palette), effects (outline/shadow), 5 built-in presets (Netflix, Prime, Disney+), custom saving/renaming/deleting, clipboard JSON import/export, and Room DB auto-saving.
- **Watch Progress** — Auto-saves every 15s, on pause, and at 90% completion (marks as watched). Resume overlay with saved position. Continue Watching section on Home.
- **DRM** — License URL + scheme passthrough on Android via Media3. iOS logs a warning (Widevine unsupported on AVPlayer).
- **Stream Switching** — Switch sources mid-playback. Position preserved and auto-sought on the new stream.
- **Google Cast** — Chromecast support on Android via Media3 Cast + MediaRouter.
- **Formats** — HLS, DASH, SmoothStreaming, progressive download. Adaptive bitrate support.

### 🎨 UI & Personalization
- **5 Themes** — Nuvio Dark, Dark, AMOLED Dark, Light, Ocean Dark.
- **10 Accent Colors** — Red, Blue, Purple, Green, Orange, Pink, Teal, Yellow, Cyan, Rose.
- **Multi-Profile** — Create and switch profiles. Addons, favorites, watch history, and settings are isolated per profile.
- **Search** — Cross-addon search with DB-persisted history and recent searches.
- **Offline Downloads** — Queue, pause, resume, cancel downloads per profile.

### 🏗 Architecture
- **96%+ Common Code** — Business logic, networking, UI, and database are entirely in `commonMain`. Platform code is limited to video rendering and file I/O.
- **Clean Module Hierarchy** — `core/*` → `feature/*` → `navigation` → `di` → `composeApp`. Core never depends on feature.
- **Offline-First** — Room-backed cache with TTL-based eviction. All Stremio responses cached locally with configurable TTLs (10min catalogs, 1hr metadata, 20min streams).
- **Reactive** — Koin DI with `StateFlow`-driven ViewModels. Compose recombines on state changes automatically.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | [Kotlin](https://kotlinlang.org/) 2.3.21 — Multiplatform, 96%+ commonMain |
| UI | [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) 1.11.0 — Material 3, shared UI across Android & iOS |
| Navigation | [Navigation Compose](https://developer.android.com/develop/navigation) 2.9.2 — Type-safe routes with `@Serializable` sealed interfaces |
| DI | [Koin](https://insert-koin.io/) 4.2.1 — Multiplatform DI with compose integration |
| Networking | [Ktor](https://ktor.io/) 3.5.0 — Content negotiation, retry with exponential backoff, gzip/deflate compression |
| Database | [Room3](https://developer.android.com/kotlin/multiplatform/room) 3.0.0-alpha05 — KMP SQLite ORM with KSP annotation processing |
| Image Loading | [Kamel](https://github.com/Kamel-Media/Kamel) 1.0.9 — Async image loading with SVG decoder support |
| Video (Android) | [Media3 / ExoPlayer](https://developer.android.com/media/media3) 1.10.1 — HLS, DASH, SmoothStreaming, RTSP, Cast |
| Video (iOS) | [AVFoundation](https://developer.apple.com/av-foundation/) — AVPlayer + AVPlayerLayer via UIKitView |
| JS Runtime | [QuickJS-KT](https://github.com/partouf/quickjs-kt) 1.0.5 — Sandboxed JavaScript with native bridge |
| HTML Parsing | [Ksoup](https://github.com/fleeksoft/ksoup) 0.2.6 — JVM cheerio-compatible HTML parser |
| Backend/Auth | [Supabase](https://supabase.com/) 3.6.0 — PostgREST + Auth SDK |
| Logging | [Kermit](https://github.com/touchlab/Kermit) 2.1.0 — Multiplatform logging |
| Serialization | [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) 1.11.0 — JSON with `ignoreUnknownKeys`, `coerceInputValues` |
| Build | AGP 9.2.1 — Gradle configuration cache, parallel builds, KSP 2.3.7 |

## Project Structure

```
MovieHub/
├── composeApp/                 # App entry point & Compose root
│   ├── commonMain/             # App.kt, Kamel config, navigation host
│   ├── androidMain/            # MainActivity (edge-to-edge, 120Hz)
│   └── iosMain/                # MainViewController (ComposeUIViewController)
│
├── di/                         # Koin DI wiring for all modules
├── navigation/                 # Type-safe route definitions + RootNavGraph
│
├── core/
│   ├── model/                  # Domain models (pure Kotlin, zero dependencies)
│   ├── network/                # Ktor client, Stremio API, AddonManager,
│   │                           # ScraperManager/PluginRuntime (QuickJS),
│   │                           # TMDB client, YouTube resolver
│   ├── database/               # Room3 database (9 entities, version 6)
│   ├── ui/                     # Shared design system + theme engine
│   └── utils/                  # Shared utilities
│
├── feature/
│   ├── home/                   # Dynamic catalogs, hero carousel, continue watching
│   ├── search/                 # Cross-addon search, search history
│   ├── details/                # Metadata display, cast, streams, trailers, series
│   ├── player/                 # Video player (ExoPlayer / AVPlayer)
│   ├── addon/                  # Addon installation & management
│   ├── auth/                   # Supabase authentication
│   ├── profile/                # Profile management, settings, appearance
│   └── sync/                   # Cloud sync (Supabase)
│
├── gradle/
│   └── libs.versions.toml      # Centralized version catalog (120+ deps)
│
└── iosApp/                     # iOS Xcode project wrapper
```

## Getting Started

### Prerequisites

- **Android Studio** Koala or later (or IntelliJ IDEA with KMP plugin)
- **JDK 17+**
- **Xcode 15+** (for iOS builds)
- A **Stremio addon manifest URL** to get started

### Clone & Build

```bash
git clone https://github.com/Sumit123rox/MovieHub.git
cd MovieHub

# Android (debug APK)
./gradlew :composeApp:assembleDebug

# iOS — compile shared Kotlin, then open in Xcode
./gradlew :composeApp:iosSimulatorArm64MainKotlinNativeCompile
open iosApp/iosApp.xcodeproj
```

### First Run

1. Launch the app. You'll be prompted to create a profile.
2. Go to the **Addons** tab.
3. Paste a Stremio addon manifest URL (e.g., `https://v3-cinemeta.strem.io/manifest.json`) and tap Install.
4. Go to the **Home** tab — catalogs from your addons will appear.
5. Browse, search, tap a title, pick a stream, and play.

> **MovieHub ships with zero built-in content.** You must install addons to see anything.

### Optional Configuration

- **TMDB API Key** — Settings → enter your TMDB API key for enriched metadata (cast photos, trailers, recommendations, age ratings).
- **Themes & Accents** — Settings → Appearance to customize.
- **Supabase** — Update your project URL and anon key in `di/src/commonMain/.../Koin.kt` for cloud sync and auth.

## Database

Room3 KMP database (version 6, `fallbackToDestructiveMigration`):

| Entity | Purpose |
|--------|---------|
| `profiles` | User profiles (name, avatar, PIN) |
| `addon` | Installed addon manifests per profile |
| `favorites` | Favorited content per profile |
| `watch_history` | Watch history entries per profile |
| `watch_progress` | Playback positions, track preferences |
| `stremio_cache` | TTL-cached Stremio API responses |
| `search_history` | Search queries per profile |
| `user_preferences` | Theme, accent, language, TMDB key |
| `downloads` | Offline download queue per profile |

## Player Architecture

Both platforms share the same Compose UI shell (`PlayerScreen.kt`) with platform-specific rendering:

| Aspect | Android | iOS |
|--------|---------|-----|
| Engine | ExoPlayer (Media3) | AVFoundation AVPlayer |
| Rendering | `AndroidView` + `PlayerView` | `UIKitView` + `AVPlayerLayer` |
| Position | 1s polling via `Player.Listener` | 1s via `addPeriodicTimeObserverForInterval` |
| Formats | HLS, DASH, SmoothStreaming, RTSP | HLS, progressive MP4 |
| DRM | Widevine via Media3 | N/A (logged warning) |
| Cast | Google Cast (Chromecast) | N/A |
| Data Source | OkHttp with proxy header support | AVURLAsset with custom headers |

## Contributing

This is a personal project. PRs and ideas are welcome — open an issue first to discuss what you'd like to change.

## License

MIT License — see [LICENSE](LICENSE).

---

<p align="center">
Made with ❤️ for the Stremio community<br>
<i>But the journey never ends.</i>
</p>
