# SysMetrics

**Real-Time Android TV System Monitor**

A high-performance system monitoring application for Android TV that displays CPU, memory, and temperature metrics in a floating overlay window. Built with modern Android architecture (MVVM + Clean Architecture), optimized for minimal resource usage.

![Android](https://img.shields.io/badge/Android-5.0%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

## Features

- **Real-time CPU monitoring** - Overall CPU usage percentage with color-coded indicators
- **RAM usage tracking** - Used/Total memory in MB with percentage display
- **Temperature monitoring** - CPU temperature from thermal zones (when available)
- **Configurable overlay** - Position, opacity, and update interval settings
- **Minimal resource usage** - < 50MB RAM, < 2% CPU overhead in idle
- **Android TV optimized** - Leanback launcher support, TV-friendly UI
- **Modern architecture** - MVVM + Clean Architecture + Coroutines

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
| **Language** | Kotlin 1.9 |
| **Min SDK** | 21 (Android 5.0) |
| **Target SDK** | 34 (Android 14) |
| **Architecture** | MVVM + Clean Architecture |
| **Async** | Kotlin Coroutines + Flow |
| **DI** | Hilt |
| **Storage** | DataStore Preferences |
| **UI** | View Binding + Custom Views |
| **Logging** | Timber |
| **Testing** | JUnit4, Mockito, Turbine |

## Project Structure

```
app/src/main/java/com/sysmetrics/app/
├── data/
│   ├── model/           # Data models (SystemMetrics, CpuStats, etc.)
│   ├── repository/      # Repository implementations
│   └── source/          # Data sources (System, Preferences)
├── domain/
│   └── usecase/         # Business logic use cases
├── ui/
│   ├── overlay/         # Overlay-specific UI components
│   ├── MainActivity.kt  # Main dashboard
│   ├── SettingsActivity.kt
│   └── *ViewModel.kt    # ViewModels
├── service/
│   └── OverlayService.kt # Foreground service for overlay
├── di/
│   └── AppModule.kt     # Hilt dependency injection
└── SysMetricsApp.kt     # Application class
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

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

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
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

## Roadmap

- [ ] Per-core CPU usage display
- [ ] GPU monitoring (device-specific)
- [ ] Network traffic monitoring
- [ ] Draggable overlay positioning
- [ ] Custom themes
- [ ] Widget support
- [ ] JNI optimization for high-frequency updates

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Android documentation for WindowManager and system metrics
- Kotlin Coroutines for reactive programming patterns
- Hilt for simplified dependency injection
