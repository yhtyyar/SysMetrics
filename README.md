# SysMetrics Pro

<div align="center">

![SysMetrics](https://img.shields.io/badge/SysMetrics-Pro-blue?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-2.7.0-green?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-5.0%2B-brightgreen?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?style=for-the-badge)

**Real-time Android System Monitor with Floating Overlay**

[Features](#-features) • [Installation](#-installation) • [Build](#-build-instructions) • [Architecture](#-architecture) • [License](#-license)

</div>

---

## 📱 Overview

SysMetrics Pro is a high-performance Android system monitoring application that displays real-time CPU, RAM, temperature, network, and battery metrics in a floating overlay window. Built with modern Android architecture (MVVM + Clean Architecture), optimized for minimal resource usage.

### Key Highlights

- 🚀 **Native C++ performance** — 10x faster metrics collection via JNI
- 📊 **Real-time monitoring** — CPU, RAM, Temperature, Network, Battery
- 🎯 **Floating overlay** — Always visible on top of other apps
- 💾 **24-hour history** — Room database with auto-cleanup
- 📤 **Data export** — CSV/JSON export with share functionality
- 🔧 **Home widget** — Quick metrics view on launcher
- ⚡ **Low overhead** — <50MB RAM, <2% CPU usage

---

## ✨ Features

| Feature | Status | Description |
|---------|:------:|-------------|
| CPU Monitoring | ✅ | Real-time CPU usage with per-core support |
| RAM Tracking | ✅ | Used/Total memory with percentage |
| Temperature | ✅ | CPU/GPU temperature from thermal zones |
| Network Stats | ✅ | Download/Upload speed monitoring |
| Battery Info | ✅ | Level, charging status, temperature |
| Floating Overlay | ✅ | Configurable position and opacity |
| Room Database | ✅ | 24-hour metrics history storage |
| CSV/JSON Export | ✅ | Export and share metrics data |
| Home Widget | ✅ | CPU/RAM widget for home screen |
| Background Collection | ✅ | WorkManager periodic collection |
| Material 3 Theme | ✅ | Modern dark theme optimized for TV |
| Hilt DI | ✅ | Dependency injection framework |
| Native JNI | ✅ | C++ optimized metrics parsing |

---

## 📋 Requirements

| Requirement | Version |
|-------------|---------|
| Android Studio | Hedgehog (2023.1.1)+ |
| JDK | 17 |
| Android SDK | 34 |
| NDK | 25.2.9519653 |
| CMake | 3.22.1 |
| Gradle | 8.2 |

---

## 🚀 Installation

### From APK

1. Download latest APK from [Releases](https://github.com/yhtyyar/SysMetrics/releases)
2. Enable "Install from unknown sources" in Settings
3. Install the APK
4. Grant overlay permission when prompted

### From Source

```bash
git clone https://github.com/yhtyyar/SysMetrics.git
cd SysMetrics
./gradlew installDebug
```

---

## 🔨 Build Instructions

### Debug Build

```bash
# Clean and build debug APK
./gradlew clean assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

#### 1. Create Release Keystore (first time only)

```bash
keytool -genkey -v -keystore release.keystore \
  -alias sysmetrics -keyalg RSA -keysize 2048 -validity 10000
```

#### 2. Configure Signing

**Option A: Environment Variables (recommended for CI/CD)**

```bash
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="sysmetrics"
export KEY_PASSWORD="your_key_password"
```

**Option B: local.properties (local development)**

```properties
# local.properties (DO NOT commit to git!)
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=sysmetrics
KEY_PASSWORD=your_key_password
```

#### 3. Build Release APK

```bash
# Build signed release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### Build All Variants

```bash
./gradlew assemble
```

### Run Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# All tests with coverage
./gradlew testDebugUnitTest jacocoTestReport
```

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ MainActivity │  │SettingsAct │  │ MinimalistOverlay   │  │
│  │   Overlay    │  │             │  │     Service         │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
│         │                │                    │              │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌─────────▼─────────┐   │
│  │ MainViewModel│  │SettingsVM  │  │  MetricsWidget    │   │
│  └──────┬──────┘  └──────┬──────┘  └───────────────────┘   │
└─────────┼────────────────┼───────────────────────────────────┘
          │                │
┌─────────▼────────────────▼───────────────────────────────────┐
│                       DOMAIN LAYER                            │
│  ┌────────────────────┐  ┌────────────────────────────────┐  │
│  │GetSystemMetricsUse │  │  ManageOverlayConfigUseCase   │  │
│  │       Case         │  │                                │  │
│  └─────────┬──────────┘  └────────────────┬───────────────┘  │
│            │                              │                   │
│  ┌─────────▼──────────┐  ┌────────────────▼───────────────┐  │
│  │ExportMetricsUseCase│  │   IMetricsHistoryRepository   │  │
│  └────────────────────┘  └────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
          │
┌─────────▼────────────────────────────────────────────────────┐
│                        DATA LAYER                             │
│  ┌────────────────────┐  ┌────────────────────────────────┐  │
│  │SystemMetricsRepo   │  │   MetricsHistoryRepository    │  │
│  └─────────┬──────────┘  └────────────────┬───────────────┘  │
│            │                              │                   │
│  ┌─────────▼──────────┐  ┌────────────────▼───────────────┐  │
│  │  SystemDataSource  │  │     MetricsDatabase (Room)    │  │
│  │  (/proc, /sys)     │  │                                │  │
│  └────────────────────┘  └────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
app/src/main/
├── cpp/                          # Native C++ code (JNI)
│   ├── CMakeLists.txt
│   └── native_metrics.cpp
├── java/com/sysmetrics/app/
│   ├── core/
│   │   ├── common/               # Constants, Result wrapper
│   │   ├── di/                   # Hilt modules, AppContainer
│   │   └── SysMetricsApplication.kt
│   ├── data/
│   │   ├── local/                # Room Database
│   │   │   ├── dao/              # MetricsHistoryDao
│   │   │   ├── entity/           # MetricsHistoryEntity
│   │   │   └── MetricsDatabase.kt
│   │   ├── model/                # Data models
│   │   ├── repository/           # Repository implementations
│   │   └── source/               # Data sources
│   ├── domain/
│   │   ├── repository/           # Repository interfaces
│   │   └── usecase/              # Business logic
│   ├── service/
│   │   └── MinimalistOverlayService.kt
│   ├── ui/
│   │   ├── MainActivityOverlay.kt
│   │   ├── SettingsActivity.kt
│   │   └── MainViewModel.kt
│   ├── widget/
│   │   └── MetricsWidgetProvider.kt
│   └── worker/
│       └── MetricsCollectionWorker.kt
└── res/
    ├── layout/
    ├── values/
    └── xml/
```

---

## 🔧 Configuration

### Overlay Settings

| Option | Values | Default |
|--------|--------|---------|
| Position | Top-Left, Top-Right, Bottom-Left, Bottom-Right | Top-Left |
| Update Interval | 500ms, 1000ms, 2000ms | 1000ms |
| Opacity | 30% - 100% | 85% |
| Show CPU | On/Off | On |
| Show RAM | On/Off | On |
| Show Time | On/Off | On |

### Background Collection

Enable in Settings → Background Collection to collect metrics every minute for 24-hour history.

### Data Export

Settings → Export CSV / Export JSON to export and share metrics history.

---

## 📊 Performance

| Metric | Target | Actual |
|--------|--------|--------|
| Memory Usage | <50MB | ~35MB |
| CPU Overhead | <2% | ~1% |
| Metrics Update | <16ms | ~5ms |
| Native Parsing | <1ms | ~0.1ms |
| APK Size | <15MB | ~10MB |

---

## 🔐 Permissions

| Permission | Purpose |
|------------|---------|
| `SYSTEM_ALERT_WINDOW` | Display floating overlay |
| `FOREGROUND_SERVICE` | Keep monitoring service running |
| `POST_NOTIFICATIONS` | Show service notification (Android 13+) |

---

## 🛠 Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 1.9, C++ 17 |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 34 (Android 14) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.48 |
| Database | Room 2.6.1 |
| Async | Coroutines + Flow |
| Background | WorkManager 2.9.0 |
| Native | NDK + CMake + JNI |
| Logging | Timber |
| Testing | JUnit4, MockK, Turbine |

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [REQUIREMENTS.md](REQUIREMENTS.md) | Product requirements and specifications |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Development guide and code standards |
| [CHANGELOG.md](CHANGELOG.md) | Version history and release notes |
| [docs/](docs/) | Additional documentation and archives |

---

<div align="center">

**Made with ❤️ for Android**

</div>
