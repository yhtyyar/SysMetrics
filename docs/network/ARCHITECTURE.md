# 🏗️ Network Traffic Monitoring - Architecture

## Overview

The Network Traffic Monitoring feature follows Clean Architecture with MVVM pattern, ensuring separation of concerns, testability, and maintainability.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │NetworkStatsVM   │  │NetworkStatsFragm│  │NetworkOverlayV  │ │
│  │                 │  │                 │  │                 │ │
│  │ - uiState       │  │ - RecyclerView  │  │ - Custom draw   │ │
│  │ - alerts        │  │ - SwipeRefresh  │  │ - Display modes │ │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘ │
└───────────┼─────────────────────┼─────────────────────┼─────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                               │
│  ┌─────────────────────────┐  ┌─────────────────────────────┐  │
│  │ GetNetworkStatsUseCase  │  │ MonitorNetworkTrafficUseCase│  │
│  │                         │  │                             │  │
│  │ - Get snapshot          │  │ - Observe traffic Flow      │  │
│  │ - Validate data         │  │ - Observe alerts            │  │
│  │ - Transform for UI      │  │ - Manage baseline           │  │
│  └───────────┬─────────────┘  └──────────────┬──────────────┘  │
└──────────────┼───────────────────────────────┼──────────────────┘
               │                               │
               ▼                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DATA LAYER                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              NetworkStatsRepository                      │   │
│  │                                                          │   │
│  │  - Aggregate data from multiple sources                  │   │
│  │  - Manage peak tracking                                  │   │
│  │  - Handle alerts                                         │   │
│  │  - Provide reactive Flows                                │   │
│  └────────────────────────┬─────────────────────────────────┘   │
│                           │                                     │
│  ┌────────────────────────┼─────────────────────────────────┐   │
│  │                        ▼                                 │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │   │
│  │  │NetworkStats  │  │NetworkType   │  │PerAppTraffic │   │   │
│  │  │DataSource    │  │Detector      │  │DataSource    │   │   │
│  │  │              │  │              │  │              │   │   │
│  │  │/proc/net/dev │  │Connectivity  │  │xt_qtaguid   │   │   │
│  │  │parsing       │  │Manager       │  │TrafficStats  │   │   │
│  │  └──────┬───────┘  └──────────────┘  └──────────────┘   │   │
│  │         │                                                │   │
│  │         ▼                                                │   │
│  │  ┌──────────────────────────────────────────────────┐   │   │
│  │  │           NativeNetworkMetrics (JNI)             │   │   │
│  │  │                                                   │   │   │
│  │  │  - C++ optimized parsing                          │   │   │
│  │  │  - Zero GC allocations                            │   │   │
│  │  │  - ~10x faster than Kotlin                        │   │   │
│  │  └──────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### Presentation Layer

| Component | Responsibility |
|-----------|----------------|
| `NetworkStatsViewModel` | Manages UI state, coordinates use cases, handles lifecycle |
| `NetworkStatsFragment` | Displays detailed stats, RecyclerView for per-app data |
| `NetworkOverlayView` | Custom View for floating overlay with multiple display modes |
| `NetworkSettingsFragment` | User preferences and alert configuration |

### Domain Layer

| Component | Responsibility |
|-----------|----------------|
| `INetworkStatsRepository` | Repository interface (abstraction) |
| `GetNetworkStatsUseCase` | Single snapshot retrieval, validation, transformation |
| `MonitorNetworkTrafficUseCase` | Continuous monitoring Flow, alert generation |

### Data Layer

| Component | Responsibility |
|-----------|----------------|
| `NetworkStatsRepository` | Implementation, aggregates data sources |
| `NetworkStatsDataSource` | Kotlin /proc/net/dev parsing |
| `NetworkTypeDetector` | ConnectivityManager integration |
| `PerAppTrafficDataSource` | Per-UID traffic via TrafficStats/xt_qtaguid |
| `NativeNetworkMetrics` | JNI bridge to C++ implementation |

## Data Flow

```
1. UI requests data (one-time or Flow)
   │
   ▼
2. ViewModel invokes UseCase
   │
   ▼
3. UseCase calls Repository
   │
   ▼
4. Repository checks native availability
   │
   ├─► Native available: Call NativeNetworkMetrics
   │   │
   │   ▼
   │   C++ reads /proc/net/dev (optimized)
   │
   └─► Native unavailable: Call NetworkStatsDataSource
       │
       ▼
       Kotlin reads /proc/net/dev (fallback)
   │
   ▼
5. Repository calculates delta, updates peaks
   │
   ▼
6. Repository emits via Flow
   │
   ▼
7. ViewModel updates StateFlow
   │
   ▼
8. UI observes and renders
```

## Key Design Decisions

### 1. Native C++ Optimization

**Decision**: Use JNI for /proc/net/dev parsing

**Rationale**:
- ~10x faster than Kotlin implementation
- Zero GC allocations on hot path
- Critical for maintaining <1% CPU overhead
- Graceful fallback when native unavailable

### 2. Snapshot Pattern for Delta Calculation

**Decision**: Store previous snapshot and calculate delta

**Rationale**:
- /proc/net/dev contains cumulative bytes
- Delta = current - previous gives speed
- Handles interface appear/disappear gracefully
- Consistent with industry standard

### 3. Flow-based Reactive API

**Decision**: Use Kotlin Flow throughout

**Rationale**:
- Natural fit for continuous monitoring
- Backpressure handling built-in
- Easy composition with combine/map
- Lifecycle-aware collection in UI

### 4. Multiple Data Sources

**Decision**: Support both /proc/net/dev and TrafficStats API

**Rationale**:
- /proc/net/dev more accurate but needs file access
- TrafficStats API works universally
- Graceful degradation if /proc unavailable
- Per-app stats require different source

### 5. Display Mode Abstraction

**Decision**: Enum-based display modes (Compact, Extended, Per-App, Combined)

**Rationale**:
- Single overlay view supports all modes
- Easy to add new modes
- User preference persistence
- Consistent formatting logic

## Module Dependencies

```
NetworkModule
├── NativeNetworkMetrics
├── NetworkStatsDataSource
│   └── DispatcherProvider
├── NetworkTypeDetector
│   └── Context (ConnectivityManager)
├── PerAppTrafficDataSource
│   ├── Context (PackageManager)
│   └── DispatcherProvider
└── NetworkStatsRepository
    ├── All DataSources
    └── DataStore (for alerts config)
```

## Threading Model

| Operation | Thread | Rationale |
|-----------|--------|-----------|
| /proc parsing | IO Dispatcher | File I/O |
| Native JNI calls | IO Dispatcher | Blocking native code |
| Delta calculation | IO Dispatcher | CPU-bound |
| UI updates | Main Dispatcher | UI thread safety |
| Flow emissions | Any | Backpressure handled |

## Error Handling Strategy

1. **Data Source Level**: Return empty/default on error, log with Timber
2. **Repository Level**: Catch exceptions, emit fallback values
3. **UseCase Level**: Validate data, filter invalid
4. **ViewModel Level**: Update error state, show Snackbar
5. **UI Level**: Display graceful degradation message

## Performance Targets

| Metric | Target | Implementation |
|--------|--------|----------------|
| CPU overhead | <1% | Native C++ parsing |
| Memory overhead | <20MB | Object pooling, weak refs for icons |
| Parsing cycle | <100ms | Native ~10ms, Kotlin ~50ms |
| UI update lag | <16ms | Background computation, main thread emit |
| Battery impact | <0.5%/24h | Configurable update interval |

## Testing Strategy

| Layer | Test Type | Tools |
|-------|-----------|-------|
| Data Sources | Unit | JUnit, MockK |
| Repository | Unit + Integration | JUnit, MockK, Turbine |
| Use Cases | Unit | JUnit, MockK |
| ViewModel | Unit | JUnit, MockK, Turbine |
| Native | Benchmark | AndroidBenchmark |
| UI | Instrumentation | Espresso |

## Security Considerations

1. **File Access**: Only read /proc files, never write
2. **Permissions**: Gracefully handle denied permissions
3. **Per-App Data**: Respect user privacy, cache only necessary data
4. **Alert Config**: Stored locally in encrypted DataStore

## Future Extensibility

- **Historical Data**: Add Room database for 24h/7d/30d history
- **Export**: CSV/JSON export of traffic data
- **Widgets**: Home screen widget support
- **Anomaly Detection**: ML-based unusual traffic detection
- **VPN Detection**: Special handling for VPN traffic

---

*Architecture designed following Clean Architecture principles for Android*
*Last updated: December 2025*
