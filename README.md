# SysMetrics

<div align="center">

[![CI](https://github.com/yhtyyar/SysMetrics/actions/workflows/ci.yml/badge.svg)](https://github.com/yhtyyar/SysMetrics/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-5.0%2B-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-purple.svg)](https://kotlinlang.org)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)

**Real-time Android system monitor with a floating overlay, native C++ backend, and home-screen widget.**

[Features](#features) · [Quick Start](#quick-start) · [Build](#build) · [Architecture](#architecture) · [Contributing](#contributing) · [License](#license)

</div>

---

## Features

- **Real-time overlay** — CPU, RAM, temperature, network speed, battery level in a draggable floating window
- **Native C++ metrics** — JNI bridge to `/proc` and `/sys` for 10x faster parsing than pure Kotlin
- **24-hour history** — Room database with auto-cleanup and CSV/JSON export
- **Home-screen widget** — CPU and RAM at a glance without opening the app
- **Android TV support** — Leanback launcher, D-pad navigation, 10-foot UI optimized layouts
- **Low overhead** — ~35 MB RAM, <2% CPU idle, minimal battery impact

## Quick Start

### Install from APK

1. Download the latest APK from [Releases](https://github.com/yhtyyar/SysMetrics/releases)
2. Install and grant **Display over other apps** permission when prompted

### Build from Source

```bash
git clone https://github.com/yhtyyar/SysMetrics.git
cd SysMetrics
./gradlew installDebug
```

**Prerequisites:** JDK 17, Android SDK 34, NDK 25.2.9519653, CMake 3.22.1

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing — see CONTRIBUTING.md)
./gradlew assembleRelease

# Unit tests
./gradlew :app:testDebugUnitTest

# Instrumented tests (emulator required)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.notPackage=com.sysmetrics.app.benchmark

# Lint
./gradlew :app:lintDebug
```

## Architecture

MVVM + Clean Architecture with three layers:

```
Presentation          Domain                Data
─────────────         ──────────            ──────────
Activity/Fragment  →  UseCases           →  Repositories
ViewModel             Repository I/F        Room DB
Overlay Service                             /proc, /sys datasources
Widget                                      Native C++ (JNI)
```

| Layer | Responsibilities |
|-------|-----------------|
| **Presentation** | `MainActivityOverlay`, `SettingsActivity`, `MinimalistOverlayService`, `MetricsWidgetProvider`, ViewModels |
| **Domain** | `GetSystemMetricsUseCase`, `ExportMetricsUseCase`, `ManageOverlayConfigUseCase`, repository interfaces |
| **Data** | `SystemMetricsRepository`, `MetricsDatabase` (Room), `SystemDataSource`, `NativeMetrics` (JNI bridge) |

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 1.9.20, C++17 |
| SDK | Min 21, Target 34 |
| DI | Hilt 2.48 (KSP) |
| Database | Room 2.6.1 (KSP) |
| Async | Coroutines + Flow |
| Background | WorkManager 2.9.0 |
| Native | NDK 25.2 + CMake 3.22.1 |
| CI | GitHub Actions |
| Testing | JUnit 4, MockK, Espresso, AndroidX Benchmark |

## Permissions

| Permission | Why |
|------------|-----|
| `SYSTEM_ALERT_WINDOW` | Floating overlay display |
| `FOREGROUND_SERVICE` | Keep monitoring service alive |
| `POST_NOTIFICATIONS` | Service notification (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Optional auto-start on boot |

No network permissions. No analytics. All data stays on-device.

## Performance

| Metric | Target | Measured |
|--------|--------|----------|
| RAM usage | <50 MB | ~35 MB |
| CPU idle | <2% | ~1% |
| Overlay update | <16 ms | ~5 ms |
| Native parse | <1 ms | ~0.1 ms |
| APK size | <15 MB | ~10 MB |

## Project Structure

```
app/src/main/
├── cpp/                     # Native C++ (JNI metrics, network stats, analytics)
├── java/com/sysmetrics/app/
│   ├── core/                # Application, Constants, DI modules
│   ├── data/                # Repositories, Room DB, data sources
│   ├── domain/              # Use cases, repository interfaces
│   ├── native_bridge/       # Kotlin JNI wrappers
│   ├── service/             # Overlay foreground service
│   ├── ui/                  # Activities, ViewModels, fragments
│   ├── widget/              # Home-screen widget provider
│   └── worker/              # WorkManager background collection
└── res/                     # Layouts, values, XML configs
```

## Contributing

Contributions are welcome! Please read:

- [CONTRIBUTING.md](CONTRIBUTING.md) — setup, code standards, commit style
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — community guidelines
- [SECURITY.md](SECURITY.md) — vulnerability reporting

This project uses [Conventional Commits](https://conventionalcommits.org) with Android module scopes (e.g., `feat(overlay):`, `fix(native):`).

## Documentation

| Document | Description |
|----------|-------------|
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to contribute, development setup |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Code standards, templates, debugging |
| [CHANGELOG.md](CHANGELOG.md) | Version history and release notes |
| [REQUIREMENTS.md](REQUIREMENTS.md) | Product requirements |
| [SECURITY.md](SECURITY.md) | Security policy and vulnerability reporting |

## License

[MIT](LICENSE) &copy; 2025 SysMetrics
