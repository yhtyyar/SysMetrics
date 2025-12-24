# SysMetrics Pro

**Real-Time Android System Monitor with Process Analytics**

A high-performance system monitoring application for Android that displays CPU, memory, temperature, and process metrics in a floating overlay window. Built with modern Android architecture (MVVM + Clean Architecture), optimized for minimal resource usage and maximum accuracy.

![Android](https://img.shields.io/badge/Android-8.0%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

## 📚 Documentation

- **[REQUIREMENTS.md](REQUIREMENTS.md)** - Product requirements and feature specifications
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Development guide, code standards, testing
- **[CHANGELOG.md](CHANGELOG.md)** - Version history and release notes
- **[LOGGING_GUIDE.md](LOGGING_GUIDE.md)** - Comprehensive debugging guide

## ✨ Features

### Current Features (v1.5.0)
- ✅ **Real-time CPU monitoring** - Overall CPU usage with color-coded indicators
- ✅ **RAM usage tracking** - Used/Total memory in MB with percentage
- ✅ **Temperature monitoring** - CPU temperature from thermal zones
- ✅ **Configurable overlay** - Position, opacity, and update intervals
- ✅ **Minimal resource usage** - <50MB RAM, <2% CPU overhead
- ✅ **Android TV optimized** - Leanback launcher support, TV-friendly UI
- ✅ **Modern architecture** - MVVM + Clean Architecture + Coroutines
- ✅ **Native C++ optimization** - JNI-based metrics (10x faster)
- ✅ **Memory leak detection** - LeakCanary integration (debug builds)
- ✅ **Performance benchmarks** - Comprehensive validation suite
- ✅ **Structured logging** - Timber-based debugging with 15+ tags

### Planned Features (v2.0.0)
- 🚧 **Process segmentation** - Self vs Other apps separation
- 🚧 **Detailed memory analysis** - Breakdown by Native/Java Heap/Graphics
- 🚧 **24-hour history** - Room database with auto-cleanup
- 🚧 **Data export** - CSV/JSON export functionality
- 🚧 **Material 3 UI** - Modern design with dark mode
- 🚧 **Enhanced settings** - Complete configuration screen
- 🚧 **Background service** - Continuous monitoring with WorkManager

See [REQUIREMENTS.md](REQUIREMENTS.md) for detailed specifications.

## Screenshots

| Main Screen | Overlay | Settings |
|:-----------:|:-------:|:--------:|
| Dashboard with metrics preview | Floating overlay window | Configuration options |

## Architecture

The application follows Clean Architecture principles with three distinct layers:

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                  │
│  ┌─────────────┐ ┌─────────────┐ ┌───────────────┐  │
│  │ MainActivity │ │SettingsAct.│ │OverlayService │  │
│  └──────┬──────┘ └──────┬──────┘ └───────┬───────┘  │
│         │               │                │          │
│  ┌──────▼──────┐ ┌──────▼──────┐ ┌───────▼───────┐  │
│  │MainViewModel│ │SettingsVM  │ │  OverlayView  │  │
│  └──────┬──────┘ └──────┬──────┘ └───────────────┘  │
└─────────┼───────────────┼───────────────────────────┘
          │               │
┌─────────▼───────────────▼───────────────────────────┐
│                    Domain Layer                      │
│  ┌─────────────────────┐ ┌────────────────────────┐ │
│  │GetSystemMetricsUseCase│ │ManageOverlayConfigUseCase│ │
│  └──────────┬──────────┘ └───────────┬────────────┘ │
└─────────────┼────────────────────────┼──────────────┘
              │                        │
┌─────────────▼────────────────────────▼──────────────┐
│                     Data Layer                       │
│  ┌────────────────────┐ ┌────────────────────────┐  │
│  │SystemMetricsRepository│ │PreferencesRepository │  │
│  └──────────┬─────────┘ └───────────┬────────────┘  │
│             │                       │               │
│  ┌──────────▼─────────┐ ┌───────────▼────────────┐  │
│  │ SystemDataSource   │ │PreferencesDataSource   │  │
│  │ (/proc, /sys)      │ │ (DataStore)            │  │
│  └────────────────────┘ └────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 1.9, C++ 17 (JNI) |
| **Min SDK** | 21 (Android 5.0) |
| **Target SDK** | 34 (Android 14) |
| **Architecture** | MVVM + Clean Architecture |
| **Async** | Kotlin Coroutines + Flow |
| **DI** | Hilt |
| **Storage** | DataStore Preferences |
| **UI** | View Binding + Custom Views |
| **Native** | NDK, CMake, JNI |
| **Logging** | Timber |
| **Testing** | JUnit4, Mockito, Turbine, Benchmark |
| **Memory** | LeakCanary (debug) |

## Project Structure

```
app/src/main/
├── cpp/                          # Native C++ code
│   ├── CMakeLists.txt           # CMake build configuration
│   ├── native_metrics.h         # JNI function declarations
│   └── native_metrics.cpp       # High-performance metrics collection
├── java/com/sysmetrics/app/
│   ├── core/
│   │   ├── common/              # Constants, Result wrapper
│   │   ├── di/                  # DispatcherProvider
│   │   └── extensions/          # Kotlin extensions
│   ├── data/
│   │   ├── model/               # Data models
│   │   ├── repository/          # Repository implementations
│   │   └── source/              # Data sources (System, Native, Preferences)
│   ├── domain/
│   │   ├── repository/          # Repository interfaces
│   │   └── usecase/             # Business logic use cases
│   ├── native_bridge/           # Kotlin JNI bridge
│   │   └── NativeMetrics.kt     # Native library wrapper
│   ├── ui/
│   │   ├── overlay/             # Overlay UI components
│   │   ├── MainActivity.kt
│   │   └── *ViewModel.kt
│   ├── service/
│   │   └── OverlayService.kt
│   ├── di/
│   │   └── AppModule.kt
│   └── SysMetricsApp.kt
└── androidTest/
    └── benchmark/               # Performance benchmark tests
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- NDK 25.2.9519653 (for native build)
- CMake 3.22.1

### Build

1. Clone the repository:
```bash
git clone https://github.com/yhtyyar/SysMetrics.git
cd SysMetrics
```

2. Open in Android Studio and sync Gradle

3. Build the project:
```bash
./gradlew assembleDebug
```

### Install

```bash
./gradlew installDebug
```

Or use Android Studio's Run button.

## Usage

1. **Launch the app** - Open SysMetrics from your launcher
2. **Grant overlay permission** - Tap "Start Monitor" and grant the overlay permission when prompted
3. **View metrics** - The floating overlay will appear showing real-time system metrics
4. **Configure** - Tap "Settings" to customize position, opacity, and displayed metrics
5. **Stop monitoring** - Tap "Stop Monitor" to disable the overlay

## Permissions

| Permission | Purpose |
|------------|---------|
| `SYSTEM_ALERT_WINDOW` | Display overlay window on top of other apps |
| `FOREGROUND_SERVICE` | Keep monitoring service running |
| `POST_NOTIFICATIONS` | Show service notification (Android 13+) |

## System Metrics Sources

| Metric | Source |
|--------|--------|
| CPU Usage | `/proc/stat` |
| Memory | `/proc/meminfo` |
| Temperature | `/sys/class/thermal/thermal_zone*/temp` |

## Performance

The application is optimized for minimal system impact:

- **Memory**: < 50MB working set
- **CPU**: < 2% in idle monitoring mode
- **Battery**: Negligible impact with 1s update interval
- **No network**: All data sourced locally

### Native Optimization

The app includes optional C++ native code for high-performance metrics collection:

| Operation | Kotlin | Native (C++) | Improvement |
|-----------|--------|--------------|-------------|
| CPU parsing | ~0.5ms | ~0.05ms | **10x faster** |
| Memory parsing | ~1ms | ~0.1ms | **10x faster** |
| Full collection | ~3ms | ~0.3ms | **10x faster** |

Native code automatically falls back to Kotlin if unavailable.

### Memory Leak Detection

LeakCanary is integrated in debug builds for automatic memory leak detection:
- Automatically detects Activity/Fragment leaks
- Monitors Service and ViewModel lifecycle
- Provides detailed leak traces in notification

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

Run benchmark tests:
```bash
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sysmetrics.app.benchmark.MetricsParserBenchmark
```

## Configuration Options

| Option | Values | Default |
|--------|--------|---------|
| Position | Top-Left, Top-Right, Bottom-Left, Bottom-Right | Top-Left |
| Update Interval | 500ms, 1000ms, 2000ms | 1000ms |
| Opacity | 30% - 100% | 85% |
| Show CPU | On/Off | On |
| Show RAM | On/Off | On |
| Show Temperature | On/Off | On |

## Debugging & Logging

SysMetrics uses structured logging with tags for easy debugging. 

**📖 Quick Start:** [QUICK_START_LOGGING.md](QUICK_START_LOGGING.md) - Start monitoring in 30 seconds  
**📚 Full Guide:** [LOGGING_GUIDE.md](LOGGING_GUIDE.md) - Comprehensive 450+ line documentation

### Quick Debug Commands

**Monitor what's displayed on screen:**
```bash
adb logcat -s OVERLAY_DISPLAY:D
```

**Debug CPU calculation issues:**
```bash
adb logcat -s METRICS_CPU:D METRICS_BASELINE:D
```

**Check top apps collection:**
```bash
adb logcat -s PROC_TOP:D
```

**Monitor all SysMetrics activity:**
```bash
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_"
```

### Logging Tags

| Tag | Purpose | Example Output |
|-----|---------|----------------|
| `OVERLAY_DISPLAY` | What's shown on screen | `📺 CPU on SCREEN: 'CPU: 45%'` |
| `OVERLAY_UPDATE` | Update cycle timing | `✅ Update cycle completed in 23ms` |
| `METRICS_CPU` | CPU calculation details | `📈 CPU: totalΔ=645 → 48.2%` |
| `PROC_TOP` | Top apps collection | `🏆 #1: YouTube - CPU=23.4%` |
| `OVERLAY_SERVICE` | Service lifecycle | `✅ Baseline ready - Initial CPU: 12.5%` |

**For complete logging documentation, troubleshooting guides, and real-world examples, see [LOGGING_GUIDE.md](LOGGING_GUIDE.md)**

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Commit Convention

This project follows [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` - New features
- `fix:` - Bug fixes
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `test:` - Test additions/changes
- `chore:` - Build/config changes

## 🗺️ Roadmap

### ✅ Completed (v1.0 - v1.5)
- [x] Real-time CPU/RAM/Temperature monitoring
- [x] Floating overlay with configurable position
- [x] Native C++ optimization (JNI bridge)
- [x] LeakCanary integration
- [x] Benchmark tests
- [x] Clean Architecture refactoring
- [x] Structured logging system

### 🚧 In Progress (v2.0)
- [ ] Process segmentation (Self vs Other)
- [ ] Room database for metrics history
- [ ] Detailed memory breakdown
- [ ] CSV/JSON export
- [ ] Material 3 UI migration
- [ ] Complete Settings screen
- [ ] Background monitoring service

### 🔮 Future (v2.1+)
- [ ] Per-core CPU usage display
- [ ] GPU monitoring (device-specific)
- [ ] Network traffic monitoring
- [ ] Draggable overlay positioning
- [ ] Custom themes and widgets
- [ ] Historical charts and trends

See [CHANGELOG.md](CHANGELOG.md) for version history.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Android documentation for WindowManager and system metrics
- Kotlin Coroutines for reactive programming patterns
- Hilt for simplified dependency injection
- Clean Architecture principles by Robert C. Martin

## 📖 Additional Resources

- **Quick Start:** [QUICK_START_LOGGING.md](QUICK_START_LOGGING.md) - Start debugging in 30 seconds
- **Development:** [DEVELOPMENT.md](DEVELOPMENT.md) - Complete development guide
- **Requirements:** [REQUIREMENTS.md](REQUIREMENTS.md) - Feature specifications
- **Release Notes:** [CHANGELOG.md](CHANGELOG.md) - Version history
