# 🤖 CLAUDE OPUS 4.5 PROMPT - Advanced Features Implementation

## Context & Task Overview

You are a **Senior Android Developer** (15+ years experience) tasked with implementing advanced monitoring and analytics features for **SysMetrics Pro** - a high-performance system metrics monitoring application for Android.

The application currently has:
- Real-time CPU/RAM/Temperature monitoring
- Floating overlay display
- Native C++ optimization via JNI
- Clean Architecture (MVVM pattern)

Your task: **Implement 7 advanced features** that expand monitoring capabilities and add analytics/export functionality.

---

## Main Task

**Generate production-ready Kotlin/Android code** for the following features:

1. **Flexible Update Intervals** - Allow users to set monitoring frequency (500ms - 5s)
2. **Peak Notifications** - Smart Toast notifications with peak stats
3. **Inline Charts** - Real-time sparkline graphs under each metric
4. **Data Export & Sharing** - Export to CSV/TXT/JSON with share intent
5. **FPS Monitoring** - Real-time FPS tracking using Choreographer API
6. **Time-Window Averages** - Calculate and display 30s/1m/5m averages
7. **Comprehensive Settings UI** - All features configurable and toggleable

**Scope:** Complete implementation from settings UI to data persistence to analytics backend.

---

## Technical Specification

**[Вставить сюда содержимое файла TECHNICAL_SPEC_Advanced_Features.md]**

---

## Implementation Requirements

### Code Quality Standards

1. **Architecture:**
   - Clean Architecture (Presentation → Domain → Data)
   - MVVM pattern with ViewModel
   - Repository pattern for data access
   - Dependency Injection (Hilt)

2. **Kotlin Standards:**
   - Kotlin 1.9+
   - Coroutines for async operations
   - Flow for reactive streams
   - Sealed classes for better type safety

3. **Android Best Practices:**
   - Jetpack components (DataStore, Room, WorkManager)
   - Material 3 Design
   - Proper lifecycle management
   - Memory-efficient implementations

4. **Performance:**
   - <100ms for settings load
   - <16ms for chart rendering (60fps)
   - <50MB additional memory
   - <2% additional CPU overhead

5. **Testing:**
   - Unit tests for all business logic (>80% coverage)
   - Integration tests for workflows
   - Performance benchmarks
   - Mock external dependencies

### Required Artifacts

Generate the following complete files:

#### 1. Data & Models
- `MonitoringSettings.kt` - Settings data class
- `TimeWindowStats.kt` - Analytics model
- `ChartDataPoint.kt` - Chart data model
- `ExportConfig.kt` - Export configuration
- `PeakStats.kt` - Peak notification data

#### 2. Settings & Preferences
- `SettingsFragment.kt` - Settings UI
- `SettingsViewModel.kt` - Settings logic
- `PreferencesRepository.kt` - Preferences access
- `PreferencesDataStore.kt` - DataStore integration
- `SettingsScreen.kt` (Compose alternative)

#### 3. Monitoring & Analytics
- `FpsMonitor.kt` - Real-time FPS tracking
- `PeakTracker.kt` - Peak value tracking
- `TimeWindowAverageCalculator.kt` - Average calculations
- `ChartDataBuffer.kt` - Circular buffer for chart data
- `JankDetector.kt` - Frame drop detection

#### 4. Notifications
- `PeakNotificationManager.kt` - Toast management
- `NotificationScheduler.kt` - Scheduled notifications

#### 5. Export & Sharing
- `MetricsExporter.kt` - Base exporter interface
- `CsvMetricsExporter.kt` - CSV format
- `TxtMetricsExporter.kt` - Text format
- `JsonMetricsExporter.kt` - JSON format
- `ExportManager.kt` - Orchestration

#### 6. UI Components
- `InlineChartView.kt` - Custom chart view
- `StatsPanel.kt` - Statistics display
- `ChartConfigurationView.kt` - Chart settings UI
- `AnalyticsFragment.kt` - Full analytics screen

#### 7. Integration & Updates
- `MonitoringServiceExtension.kt` - Service updates
- `OverlayUpdater.kt` - Overlay integration
- Updated DI modules (Hilt)
- Updated AndroidManifest.xml (if needed)

#### 8. Testing
- `FpsMonitorTest.kt`
- `TimeWindowAverageCalculatorTest.kt`
- `MetricsExporterTest.kt`
- `SettingsViewModelTest.kt`
- `CsvMetricsExporterTest.kt` (format validation)

---

## 7-Day Implementation Plan

### Day 1: Settings Infrastructure
**Deliverables:**
- DataStore integration
- Settings models (MonitoringSettings, etc.)
- PreferencesRepository with full CRUD
- SettingsViewModel with validation
- Unit tests for repository & viewmodel

**Code to generate:**
- MonitoringSettings.kt
- PreferencesRepository.kt
- SettingsViewModel.kt
- PreferencesRepositoryTest.kt

### Day 2: Settings UI
**Deliverables:**
- SettingsFragment (XML layout + behavior)
- Material 3 styled UI
- All feature toggles
- Interval dropdown
- Preview of changes
- Unit tests for UI logic

**Code to generate:**
- SettingsFragment.kt
- activity_settings.xml / settings_screen.xml
- SettingsFragmentTest.kt

### Day 3: Monitoring Configuration & Peak Tracking
**Deliverables:**
- Update interval control in service
- PeakTracker implementation
- PeakNotificationManager
- Toast generation logic
- Scheduled notifications

**Code to generate:**
- PeakTracker.kt
- PeakNotificationManager.kt
- MonitoringServiceExtension.kt
- PeakTrackerTest.kt

### Day 4: Analytics Backend (Averages & Stats)
**Deliverables:**
- TimeWindowAverageCalculator
- ChartDataBuffer (circular buffer)
- Percentile calculations
- Memory-efficient data retention
- Unit tests with various window sizes

**Code to generate:**
- TimeWindowAverageCalculator.kt
- ChartDataBuffer.kt
- TimeWindowAverageCalculatorTest.kt
- ChartDataBufferTest.kt

### Day 5: FPS Monitoring & Jank Detection
**Deliverables:**
- FpsMonitor with Choreographer API
- Frame counting logic
- JankDetector for frame drops
- FPS status indicators
- Integration with settings

**Code to generate:**
- FpsMonitor.kt
- JankDetector.kt
- FpsMonitorTest.kt

### Day 6: Data Export & Sharing
**Deliverables:**
- CsvMetricsExporter (valid CSV format)
- TxtMetricsExporter (formatted text)
- JsonMetricsExporter (valid JSON)
- ExportManager orchestration
- Share intent integration
- Unit tests for all exporters

**Code to generate:**
- MetricsExporter.kt (interface)
- CsvMetricsExporter.kt
- TxtMetricsExporter.kt
- JsonMetricsExporter.kt
- ExportManager.kt
- ExportManagerTest.kt
- *ExporterTest.kt (for each format)

### Day 7: Inline Charts & UI Integration
**Deliverables:**
- InlineChartView custom component
- Chart rendering logic (smooth, performant)
- Per-chart toggle support
- StatsPanel for aggregated stats
- Analytics screen with all data
- Integration with overlay
- Performance benchmarks

**Code to generate:**
- InlineChartView.kt
- StatsPanel.kt
- AnalyticsFragment.kt
- fragment_analytics.xml
- Updated layout files (overlay, main)
- AnalyticsFragmentTest.kt

---

## Expected Code Quality

### Architecture Pattern
```
Presentation Layer:
├── SettingsFragment / SettingsScreen
├── AnalyticsFragment
└── SettingsViewModel / AnalyticsViewModel

Domain Layer:
├── UpdateIntervalUseCase
├── ExportDataUseCase
└── GetAnalyticsUseCase

Data Layer:
├── PreferencesRepository
├── MetricsRepository
└── ExportRepository
```

### Kotlin Best Practices
```kotlin
// Use sealed classes for type safety
sealed class ExportFormat {
    object Csv : ExportFormat()
    object Txt : ExportFormat()
    object Json : ExportFormat()
}

// Use Flow for reactive data
class TimeWindowAverageCalculator {
    val averageFlow: Flow<TimeWindowStats> = ...
}

// Use coroutines for async work
suspend fun exportMetrics(config: ExportConfig): File = withContext(Dispatchers.IO) {
    // Implementation
}

// Proper nullability handling
data class ChartDataPoint(
    val timestamp: Long,
    val value: Float,
    val severity: Severity
)
```

### Error Handling
- Graceful degradation (e.g., FPS monitoring fallback)
- Try-catch for file operations
- Validation of user inputs
- Proper logging

---

## Success Criteria

✅ All 7 features fully implemented  
✅ Settings persist across app restarts  
✅ Update intervals work correctly (±50ms tolerance)  
✅ Notifications appear on schedule  
✅ Charts render at 60fps  
✅ FPS monitoring accurate (±2fps)  
✅ Exports generate valid CSV/TXT/JSON files  
✅ Sharing works on all Android versions  
✅ <50MB additional memory usage  
✅ <2% additional CPU overhead  
✅ >80% code coverage with tests  
✅ All code follows Kotlin style guide  
✅ No memory leaks in long-running operations  
✅ Proper resource cleanup (listeners, callbacks)  

---

## Special Requirements

### 1. FPS Monitoring Specifics
- Use Choreographer.getInstance() on main thread
- Count frames per second accurately
- Detect jank (frames >16.6ms on 60hz)
- Handle device refresh rates (60/90/120hz)
- Minimal CPU overhead (<1%)

### 2. Chart Rendering
- Use MPAndroidChart OR custom Canvas implementation
- Implement Bézier smoothing for curves
- Color gradients: Green → Yellow → Red
- Support different chart heights
- No memory leaks on configuration changes

### 3. Data Export Formats

**CSV:**
```csv
timestamp,cpu_percent,ram_mb,ram_percent,temp_celsius,net_ingress_mbps,net_egress_mbps,fps
2025-12-19T14:23:45Z,45,523,25.5,38,2.5,0.8,59
```

**TXT:**
```
═════════════════════════════════════════
SysMetrics Pro - Performance Report
Generated: 2025-12-19 14:23:45 UTC
═════════════════════════════════════════

SUMMARY
CPU Average: 45.2% (Min: 12.5%, Max: 92.1%)
RAM Average: 523.4 MB (Min: 450 MB, Max: 856 MB)
...
```

**JSON:**
```json
{
  "metadata": {
    "generated_at": "2025-12-19T14:23:45Z",
    "duration_seconds": 300
  },
  "summary": {
    "cpu": {"average": 45.2, "min": 12.5, "max": 92.1}
  },
  "data_points": [...]
}
```

### 4. Settings Validation
- Update interval: 500-5000ms (in 500ms increments)
- Toast duration: 3-10 seconds
- Chart height: Small (20dp) / Normal (40dp) / Large (60dp)
- Time windows: 30s, 1m, 5m (or custom)

### 5. Memory Management
- Circular buffers for data retention
- Auto-cleanup of old data points
- No memory leaks from Choreographer callbacks
- Proper cleanup on Fragment destroy
- Unregister listeners/callbacks

### 6. Testing Requirements
- Unit test all calculations
- Test edge cases (empty data, single point, etc.)
- Test CSV/JSON/TXT format validity
- Test Settings persistence
- Benchmark chart rendering
- Mock System APIs

---

## References & Resources

### Android Documentation
- [DataStore Guide](https://developer.android.com/topic/libraries/architecture/datastore)
- [Choreographer API](https://developer.android.com/reference/android/view/Choreographer)
- [Frame Rate Basics](https://developer.android.com/guide/topics/graphics/frame-rate)
- [WorkManager for Scheduled Tasks](https://developer.android.com/develop/background-work/background-tasks/persistent-work)

### Libraries
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - Charts
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) - Settings
- [Room Database](https://developer.android.com/training/data-storage/room) - Persistent storage
- [Hilt](https://dagger.dev/hilt/) - Dependency injection

### Best Practices
- [Android Architecture](https://developer.android.com/topic/architecture)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Testing on Android](https://developer.android.com/training/testing)

---

## Deliverables Checklist

- [ ] All 30+ source files generated
- [ ] All settings models complete
- [ ] PreferencesRepository fully functional
- [ ] SettingsViewModel with validation
- [ ] SettingsFragment UI functional
- [ ] FpsMonitor working with Choreographer
- [ ] TimeWindowAverageCalculator accurate
- [ ] All exporters generating valid files
- [ ] InlineChartView smooth rendering
- [ ] StatsPanel displaying aggregated data
- [ ] All 15+ unit test files complete
- [ ] Integration tests for workflows
- [ ] Performance benchmarks written
- [ ] Documentation in KDoc format
- [ ] No TODOs or placeholder code
- [ ] Ready to merge to main branch

---

## Clarification Questions (If Needed)

1. Should FPS monitoring count only UI-thread frames or include GPU rendering?
2. Chart library preference: MPAndroidChart vs custom Canvas vs Jetpack Compose?
3. Data retention policy: How long to keep historical data (7d/30d/never)?
4. Auto-export functionality: Should data auto-export periodically?
5. Multi-window support: Should stats work with split-screen?

---

**Status:** Ready for Implementation  
**Estimated Duration:** 7 days (100-150 hours)  
**Complexity:** High (6-7 interconnected features)  
**Team Size:** 1-2 Senior Android Developers  

---

**Let's build great features! 🚀**

