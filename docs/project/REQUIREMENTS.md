# SysMetrics Pro - Product Requirements

**Version:** 2.0  
**Date:** December 15, 2025  
**Status:** Ready for Implementation  

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Current State](#current-state)
3. [Required Features](#required-features)
4. [Technical Specifications](#technical-specifications)
5. [Performance Requirements](#performance-requirements)
6. [Acceptance Criteria](#acceptance-criteria)

---

## 🎯 Overview

**SysMetrics Pro** is a high-performance Android system monitoring application that provides real-time tracking of CPU, RAM, temperature, and process metrics with minimal resource overhead.

### Core Characteristics
- ✅ MVVM architecture + Clean Architecture
- ✅ Floating overlay for real-time monitoring
- ✅ CPU, RAM, temperature monitoring
- ✅ <50MB memory optimization
- ✅ Native Kotlin JNI bridge
- ✅ Comprehensive benchmark tests

---

## 📊 Current State

### ✅ Working Features
- Real-time CPU monitoring
- RAM usage tracking
- Floating overlay widget
- Optimized performance
- Native metrics via JNI
- LeakCanary integration
- Benchmark tests

### ❌ Missing Features
- Self vs Other apps process segmentation
- 24-hour metrics history (SQLite/Room)
- Data export (CSV/JSON)
- Material 3 UI components
- Detailed memory analysis by type
- Complete Settings screen
- Background monitoring service

---

## 🔧 Required Features

### 1. Process Segmentation (CRITICAL)

**Goal:** Separate metrics for own app vs other applications

**Requirements:**
- ✅ Fetch process list via `ActivityManager`
- ✅ Identify Self by PID and package name
- ✅ Classify Other apps (system/user)
- ✅ 95% metrics accuracy
- ✅ <50ms execution for 300+ processes

**Data Structure:**
```kotlin
data class ProcessSegmentation(
    val selfProcesses: List<ProcessInfo>,
    val otherProcesses: List<ProcessInfo>,
    val timestamp: Long
)

data class ProcessInfo(
    val pid: Int,
    val packageName: String,
    val processName: String,
    val memoryBytes: Long,
    val cpuPercent: Float,
    val state: ProcessState
)

enum class ProcessState {
    FOREGROUND, BACKGROUND, SERVICE, HIDDEN, ZOMBIE
}
```

---

### 2. Detailed Memory Analysis (HIGH)

**Goal:** Show memory breakdown by type

**Requirements:**
- ✅ Breakdown: Native, Java Heap, Graphics, Stack, Other
- ✅ Accuracy ±5% from Debug.MemoryInfo
- ✅ Self + top 10 other apps
- ✅ Update every 2-3 seconds

**Data Structure:**
```kotlin
data class MemoryBreakdown(
    val nativeMemory: Long,
    val javaHeap: Long,
    val graphicsMemory: Long,
    val stackMemory: Long,
    val other: Long
) {
    val totalMemory: Long = 
        nativeMemory + javaHeap + graphicsMemory + stackMemory + other
}

data class DetailedProcessMemory(
    val pid: Int,
    val packageName: String,
    val memoryBreakdown: MemoryBreakdown,
    val totalPss: Long,
    val timestamp: Long
)
```

---

### 3. Room Database for History (HIGH)

**Goal:** Store 24 hours of metrics history

**Requirements:**
- ✅ SQLite via Room ORM
- ✅ Save every 2-3 seconds
- ✅ Auto-cleanup data older than 24h
- ✅ Max 50MB disk usage
- ✅ ~30k records per day

**Entities:**
```kotlin
@Entity(tableName = "metrics_history")
data class MetricsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val cpuPercent: Float,
    val ramPercent: Float,
    val temperature: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "process_history")
data class ProcessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pid: Int,
    val packageName: String,
    val cpuPercent: Float,
    val memoryBytes: Long,
    val timestamp: Long
)
```

---

### 4. CSV/JSON Export (MEDIUM)

**Goal:** Export metrics for external analysis

**Requirements:**
- ✅ Export last 24h of metrics
- ✅ CSV format (timestamp, cpu, ram, temp)
- ✅ JSON format (structured)
- ✅ Save to Downloads folder
- ✅ Share via intent

**CSV Example:**
```csv
timestamp,cpu_percent,ram_percent,temperature
1702650240000,42.5,65.2,45.3
1702650242000,38.2,64.1,45.1
```

**JSON Example:**
```json
{
  "exportDate": "2025-12-15T16:50:00Z",
  "metrics": [
    {
      "timestamp": 1702650240000,
      "cpu": 42.5,
      "ram": 65.2,
      "temperature": 45.3
    }
  ]
}
```

---

### 5. Material 3 UI Update (MEDIUM)

**Goal:** Modernize UI with Material 3

**Requirements:**
- ✅ Material 3 color scheme
- ✅ Dark mode support
- ✅ Responsive layout (phone/tablet)
- ✅ Smooth animations (250-300ms)

**Color Palette:**
```kotlin
// Light Theme
val LightColors = lightColorScheme(
    primary = Color(0xFF2180A0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB1E5FF),
    secondary = Color(0xFF5E5260),
    tertiary = Color(0xFF7D5260)
)

// Dark Theme
val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D1E5),
    onPrimary = Color(0xFF134252),
    primaryContainer = Color(0xFF2F5D74),
    secondary = Color(0xFFCCC0CB),
    tertiary = Color(0xFFE7B7C3)
)
```

---

### 6. Settings Screen (MEDIUM)

**Goal:** Complete settings implementation

**Requirements:**
- ✅ Update interval (1-5s, default 2s)
- ✅ Overlay toggle
- ✅ Background service toggle
- ✅ Theme selector (Light/Dark/Auto)
- ✅ Data export button
- ✅ Clear history button

**UI Structure:**
```
SETTINGS
├─ Monitoring
│  ├─ Update Interval [2s]
│  ├─ Overlay Enabled [✓]
│  └─ Background Service [✓]
├─ Appearance
│  └─ Theme [∨ Auto]
├─ Data
│  ├─ Export Metrics
│  └─ Clear History
└─ About
   └─ Version 2.0
```

---

### 7. Background Service (LOW)

**Goal:** Continue monitoring in background

**Requirements:**
- ✅ Foreground Service with notification
- ✅ WorkManager for periodic tasks
- ✅ Low battery optimization (5-10s interval)
- ✅ <2% battery drain per hour

---

## 🏗️ Technical Specifications

### Architecture Pattern
```
Presentation (MVVM)
    ↓
Domain (UseCase)
    ↓
Repository
    ↓
Data Layer
├─ Room Database
├─ System APIs (/proc/stat)
├─ ActivityManager
└─ Preferences
```

### Technology Stack
- **Language:** Kotlin 1.9+
- **Framework:** Android 8.0+ (API 26+)
- **DI:** Hilt 2.48+
- **Database:** Room 2.6+
- **Async:** Coroutines 1.7+ + Flow
- **UI:** Material 3
- **Testing:** JUnit 4 + MockK

### Directory Structure
```
app/src/main/kotlin/com/sysmetrics/app/
├── data/
│   ├── datasource/
│   │   ├── ProcessDataSource.kt (NEW)
│   │   ├── MemoryDataSource.kt (NEW)
│   │   └── SystemMetricsDataSource.kt
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt (NEW)
│   │   │   ├── entity/
│   │   │   │   ├── MetricsEntity.kt (NEW)
│   │   │   │   └── ProcessEntity.kt (NEW)
│   │   │   └── dao/
│   │   │       └── MetricsDao.kt (NEW)
│   │   └── preferences/
│   │       └── PreferencesManager.kt
│   └── repository/
│       ├── ProcessRepositoryImpl.kt (UPDATE)
│       ├── HistoryRepositoryImpl.kt (NEW)
│       └── ExportRepositoryImpl.kt (NEW)
├── domain/
│   ├── repository/
│   │   ├── IProcessRepository.kt (UPDATE)
│   │   ├── IHistoryRepository.kt (NEW)
│   │   └── IExportRepository.kt (NEW)
│   ├── usecase/
│   │   ├── GetProcessListUseCase.kt (NEW)
│   │   ├── GetMemoryAnalysisUseCase.kt (NEW)
│   │   └── ExportMetricsUseCase.kt (NEW)
│   └── model/
│       ├── ProcessInfo.kt (UPDATE)
│       ├── MemoryBreakdown.kt (NEW)
│       └── ProcessSegmentation.kt (NEW)
├── ui/
│   ├── screens/
│   │   ├── dashboard/
│   │   ├── details/
│   │   └── settings/ (NEW)
│   ├── components/
│   │   ├── CpuChart.kt (NEW)
│   │   └── RamChart.kt (NEW)
│   └── theme/
│       ├── Theme.kt (UPDATE)
│       └── Color.kt (UPDATE)
├── service/
│   ├── MetricsBackgroundService.kt (NEW)
│   └── MetricsWorker.kt (NEW)
└── utils/
    └── exporters/
        ├── CsvExporter.kt (NEW)
        └── JsonExporter.kt (NEW)
```

---

## ⚡ Performance Requirements

| Metric | Target | Critical |
|--------|--------|----------|
| Metrics updates | <16ms | 60 FPS |
| Process parsing | <50ms | 300+ processes |
| Memory usage | <150MB | <200MB |
| Battery drain | <2%/hour | <3%/hour |
| Startup time | <2 sec | <3 sec |
| Database writes | <5ms | <10ms |

### Constraints
- **Min Android:** 8.0 (API 26)
- **Max Android:** 15 (API 35)
- **Low-end devices:** 2GB RAM support
- **CPU usage:** <5% during monitoring
- **Responsive:** Phone/Tablet layouts

---

## ✅ Acceptance Criteria

### Functionality
- [ ] Process segmentation (Self vs Other) works correctly
- [ ] Memory breakdown shows all types accurately
- [ ] 24h history stored in database
- [ ] CSV/JSON export functional
- [ ] Settings persist across restarts
- [ ] Background service runs reliably

### Quality
- [ ] 70%+ test coverage
- [ ] All unit tests passing
- [ ] Performance benchmarks met
- [ ] No memory leaks detected
- [ ] Code passes ktlint/detekt

### Performance
- [ ] Metrics updates: <16ms
- [ ] Process parsing: <50ms
- [ ] Memory usage: <150MB
- [ ] Battery drain: <2%/hour
- [ ] Startup time: <2 sec

### Documentation
- [ ] KDoc comments on public APIs
- [ ] Architecture diagrams updated
- [ ] README updated
- [ ] API documentation complete
- [ ] Testing guide provided

---

## 📅 Implementation Timeline

**Week 1:** Process Segmentation + Memory Analysis  
**Week 2:** Room Database + History  
**Week 3:** Material 3 UI + Settings  
**Week 4:** Export + Background Service  
**Week 5:** Testing + Optimization  
**Week 6:** Documentation + Release  

---

## 📞 References

**Repository:** https://github.com/yhtyyar/SysMetrics  
**Min API:** 26 (Android 8.0)  
**Target API:** 34 (Android 14)  

---

*Last updated: December 15, 2025*
