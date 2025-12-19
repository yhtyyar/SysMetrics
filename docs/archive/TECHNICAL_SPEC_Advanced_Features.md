# 📋 РАСШИРЕННОЕ ТЕХНИЧЕСКОЕ ЗАДАНИЕ

## Advanced Settings & Analytics Features для SysMetrics Pro

**Версия:** 2.0  
**Дата:** December 19, 2025  
**Статус:** Design & Requirements Phase  
**Приоритет:** High  
**Сложность:** High (Интеграция 6+ фич)

---

## 1. ОБЗОР И ЦЕЛЬ

### 1.1 Основная Цель

Расширить SysMetrics Pro с расширенной конфигурацией и аналитикой:

1. **Гибкие настройки мониторинга** - интервалы обновления (0.5s - 5s)
2. **Smart Notifications** - Toast с peak данными (опционально)
3. **Inline Charts** - Миниатюрные графики под каждой метрикой
4. **Data Export** - CSV/TXT с возможностью поделиться
5. **FPS Monitoring** - Real-time FPS отслеживание (Choreographer API)
6. **Time-Window Averages** - Средние значения за 30s/1m/5m
7. **Granular Feature Toggle** - Каждую фичу можно включить/выключить

### 1.2 Бизнес-ценность

- 🎯 **Более глубокий анализ** - графики показывают тренды
- 💾 **Data Export** - пользователи могут анализировать данные дальше
- ⏱️ **FPS мониторинг** - выявление UI лагов
- 🔔 **Умные оповещения** - не раздражает, но информирует
- ⚙️ **Full Control** - пользователь настраивает ВСЁ

---

## 2. ТРЕБОВАНИЯ

### 2.1 Feature 1: Flexible Update Intervals

**Требование:** Позволить пользователю выбрать частоту обновления метрик

**Dropdown Menu Options:**
```
⚡ Ultra-Fast (500ms) - для критичной отладки
🚀 Fast (1s) - стандартный режим (default)
⚖️ Balanced (2s) - для фонового мониторинга
🔋 Power Save (3s) - экономия батареи
💤 Light (5s) - минимальный overhead
```

**Implementation Details:**
- Store в DataStore Preferences
- Enum для типизации
- Validate range [500ms, 5000ms]
- Persist значение при перезапуске
- Instant apply (не требует перезагрузки приложения)

**UI/UX:**
```
Settings Screen → Performance → Update Interval
┌─────────────────────────────┐
│ Update Interval              │
├─────────────────────────────┤
│ ⦿ 500ms (Ultra-Fast)       │
│ ○ 1s (Fast) ← default      │
│ ○ 2s (Balanced)            │
│ ○ 3s (Power Save)          │
│ ○ 5s (Light)               │
└─────────────────────────────┘
```

**Acceptance Criteria:**
- ✅ Dropdown работает корректно
- ✅ Значение сохраняется в DataStore
- ✅ Изменение немедленно применяется
- ✅ Все интервалы работают без крашей
- ✅ CPU overhead соответствует интервалу

---

### 2.2 Feature 2: Peak Notifications (Toast)

**Требование:** Показывать Toast с максимальным потреблением в определенные промежутки

**Toast Content:**
```
┌──────────────────────────────┐
│ 📊 Peak Stats (Last Minute)  │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ 🔥 CPU Peak: 92% (14:23:45) │
│ 💾 RAM Peak: 856MB (14:24:10)│
│ 🌡️ Temp Peak: 42°C (14:23:50)│
│ 📡 Net Peak: 12.5 Mbps ↓     │
│                             │
│ Average: CPU 45% | RAM 520MB │
│         [Dismiss]           │
└──────────────────────────────┘
```

**Configuration:**
- **Interval Options:**
  - Per minute (default)
  - Per 30 seconds
  - Per 5 minutes
  - Custom interval

- **Enable/Disable per resource:**
  ```
  ☑ Show CPU Peak
  ☑ Show RAM Peak
  ☑ Show Temperature
  ☑ Show Network
  ☐ Show FPS
  ```

- **Toast Duration:** 3-10 seconds (configurable)

**Implementation:**
- ScheduledExecutorService для periodic уведомлений
- Track peak values в window (30s, 1m, 5m)
- Timezone-aware timestamps
- Swipeable notifications

**Acceptance Criteria:**
- ✅ Toast появляется в правильный момент
- ✅ Содержит правильные values (peak + timestamp)
- ✅ Можно настроить интервал
- ✅ Каждый ресурс можно toggle'ировать
- ✅ Не раздражает частотой

---

### 2.3 Feature 3: Inline Charts (Sparklines)

**Требование:** Показывать миниатюрные линейные графики под каждой метрикой

**Visual Design:**
```
CPU Usage: 45% ═══════════════════════════
          ┌─────────────────────────────┐
          │   ╱─╲   ╱─╲   ╱─╲           │
          │  ╱   ╲ ╱   ╲ ╱   ╲          │
          │╱       ╲     ╲     ╲        │
          └─────────────────────────────┘
          
RAM Usage: 523 MB / 2048 MB (25.5%)
          ┌─────────────────────────────┐
          │             ╱─╲             │
          │            ╱   ╲            │
          │      ─────╱     ╲──         │
          │     ╱            ╲          │
          │────╱              ╲─────    │
          └─────────────────────────────┘
```

**Technical Implementation:**
- **Data Structure:** Circular buffer (last 60 data points)
- **Chart Library:** MPAndroidChart (или custom Canvas)
- **Render Performance:** 
  - Re-render только при new data point
  - Caching для performance
  - Avoid memory leaks

- **Features:**
  - Color gradient (green → yellow → red)
  - Smooth curves (Bézier interpolation)
  - Scrollable history (swipe for older data)
  - Tap to see exact value + timestamp

**Configuration per Resource:**
```
Settings → Charts
┌──────────────────────────────┐
│ Inline Charts                │
├──────────────────────────────┤
│ ☑ CPU Chart                  │
│ ☑ RAM Chart                  │
│ ☑ Temperature Chart          │
│ ☑ Network Chart              │
│ ☑ FPS Chart                  │
│                              │
│ Chart History: Last 60 values│
│ Chart Height: Small / Normal │
└──────────────────────────────┘
```

**Size Guidelines:**
```
Normal View: 40dp height, full width
Compact View: 20dp height (for overlay)
Gesture: Long-press для full-screen chart
```

**Color Scheme:**
```
CPU:         Blue → Yellow → Red
RAM:         Green → Orange → Red
Temperature: Blue → Yellow → Red
Network:     Cyan → Green → Orange
FPS:         Green (60 fps) → Yellow (45) → Red (30)
```

**Acceptance Criteria:**
- ✅ Charts отрисовываются плавно
- ✅ Нет jank или drops
- ✅ Каждый chart можно toggle'ировать
- ✅ Цвета соответствуют severity
- ✅ Tap показывает exact values
- ✅ No memory leaks
- ✅ <10ms render time per frame

---

### 2.4 Feature 4: Data Export & Sharing

**Требование:** Экспортировать метрики в файл и поделиться

**Export Formats:**

**Format 1: CSV (spreadsheet-friendly)**
```csv
timestamp,cpu_percent,ram_mb,ram_percent,temp_celsius,net_ingress_mbps,net_egress_mbps,fps,battery_percent
2025-12-19 14:23:45,45,523,25.5,38,2.5,0.8,59,78
2025-12-19 14:23:46,47,525,25.6,38,2.3,0.7,60,78
2025-12-19 14:23:47,42,521,25.4,37,1.8,0.6,59,77
...
```

**Format 2: TXT (human-readable)**
```
═════════════════════════════════════════════════════
SysMetrics Pro - Performance Report
Generated: 2025-12-19 14:23:45 UTC
Duration: 5 minutes (300 seconds)
═════════════════════════════════════════════════════

SUMMARY STATISTICS:
─────────────────────────────────────────────────────
CPU Usage
  Average:  45.2%
  Min:      12.5%
  Max:      92.1%
  Peak Time: 14:23:52

RAM Usage
  Average:  523.4 MB (25.5%)
  Min:      450 MB (22.0%)
  Max:      856 MB (41.8%)
  Peak Time: 14:24:10

Temperature
  Average:  38.5°C
  Min:      37.0°C
  Max:      42.1°C
  Peak Time: 14:23:50

Network Traffic
  Ingress Peak:   12.5 Mbps (14:23:45)
  Egress Peak:    5.2 Mbps (14:24:05)
  Total Down:     2.3 GB
  Total Up:       450 MB

FPS
  Average:  58.2 fps
  Min:      24 fps
  Max:      60 fps
  Drops:    12 (below 30 fps)

═════════════════════════════════════════════════════
DETAILED LOG:
─────────────────────────────────────────────────────
[14:23:45] CPU: 45%, RAM: 523MB, Temp: 38°C, Net: ↓2.5M↑0.8M, FPS: 59
[14:23:46] CPU: 47%, RAM: 525MB, Temp: 38°C, Net: ↓2.3M↑0.7M, FPS: 60
...
```

**Format 3: JSON (для programmatic analysis)**
```json
{
  "metadata": {
    "generated_at": "2025-12-19T14:23:45Z",
    "duration_seconds": 300,
    "device": "Pixel 6 Pro",
    "android_version": "14",
    "app_version": "1.5.0"
  },
  "summary": {
    "cpu": {
      "average": 45.2,
      "min": 12.5,
      "max": 92.1,
      "peak_at": "2025-12-19T14:23:52Z"
    },
    "ram": {
      "average_mb": 523.4,
      "average_percent": 25.5,
      "min_mb": 450,
      "max_mb": 856,
      "peak_at": "2025-12-19T14:24:10Z"
    }
  },
  "data_points": [
    {
      "timestamp": "2025-12-19T14:23:45Z",
      "cpu_percent": 45,
      "ram_mb": 523,
      "temp_celsius": 38,
      ...
    }
  ]
}
```

**Share Destinations:**
```
Export Data
├─ 📧 Email
├─ 📱 SMS / Telegram / WhatsApp
├─ ☁️ Google Drive / OneDrive / Dropbox
├─ 📋 Copy to Clipboard
├─ 💾 Save to Downloads
└─ 🔗 Share Link (if cloud sync enabled)
```

**Implementation:**
```kotlin
// File generation
val exportFile = generateExportFile(
    format = ExportFormat.CSV, // или TXT, JSON
    range = TimeRange.LAST_5_MINUTES,
    resources = setOf(Resource.CPU, Resource.RAM, Resource.FPS)
)

// Share
val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "text/csv" // или text/plain, application/json
    putExtra(Intent.EXTRA_STREAM, exportFile.uri)
    putExtra(Intent.EXTRA_SUBJECT, "SysMetrics Report - 2025-12-19")
}
startActivity(Intent.createChooser(shareIntent, "Share Report"))
```

**Acceptance Criteria:**
- ✅ CSV экспортируется корректно
- ✅ TXT читабелен и структурирован
- ✅ JSON valid и парсируется
- ✅ Sharing работает на все платформы
- ✅ Файлы содержат правильные данные
- ✅ Timestamps UTC и timezone-aware
- ✅ Нет sensitive info в экспорте

---

### 2.5 Feature 5: FPS Monitoring (Real-Time)

**Требование:** Отслеживать FPS UI-потока в реальном времени

**Technical Approach: Choreographer API**

```kotlin
class FpsMonitor {
    private val choreographer = Choreographer.getInstance()
    private var frameCount = 0
    private var lastSecondTime = System.nanoTime()
    private val fps = MutableStateFlow(0)
    
    fun startMonitoring() {
        choreographer.postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val now = System.nanoTime()
                val elapsedNanos = now - lastSecondTime
                
                if (elapsedNanos >= 1_000_000_000) { // 1 second
                    fps.value = frameCount
                    frameCount = 0
                    lastSecondTime = now
                }
                
                choreographer.postFrameCallback(this)
            }
        })
    }
}
```

**FPS Metrics:**
```
Real-time FPS: 58 fps
├─ Status: Good (55-60 fps range)
├─ Frame Drops: 0 (below 30 fps)
├─ Average: 58.2 fps (last minute)
├─ Min: 24 fps
└─ Max: 60 fps

Visual Indicator:
  🟢 Green (55-60 fps): Smooth
  🟡 Yellow (45-54 fps): Acceptable
  🟠 Orange (30-44 fps): Needs optimization
  🔴 Red (<30 fps): Lag detected
```

**Advanced Metrics:**
```
Frame Time Analysis:
├─ 60 fps frames: 90%
├─ 30-59 fps frames: 8%
├─ <30 fps frames: 2%
└─ Total drops: 12

Jank Detection:
  Frame took 45ms (should be 16.6ms)
  Skipped frames: 2
  Jank causes: [GC pause, I/O wait, ...]
```

**Configuration:**
```
Settings → Monitoring Metrics → FPS
┌──────────────────────────────┐
│ ☑ Show FPS                   │
│ ☑ Show Frame Drops           │
│ ☑ Show Jank Warning          │
│ ☑ Show Jank Details          │
│                              │
│ FPS Threshold: 30 fps ▼      │
│ (Below this = jank alert)    │
└──────────────────────────────┘
```

**Acceptance Criteria:**
- ✅ FPS мониторится без lag
- ✅ CPU overhead < 0.5%
- ✅ Точность ±2 fps
- ✅ Frame drops detected correctly
- ✅ Jank warnings работают
- ✅ No memory leaks in Choreographer callback

**Known Limitations:**
- ⚠️ Показывает только UI-thread FPS
- ⚠️ Не учитывает GPU rendering для games
- ⚠️ На некоторых devices может быть 120hz refresh rate

---

### 2.6 Feature 6: Time-Window Averages

**Требование:** Вычислять и показывать средние значения за определенный период

**Time Windows:**
```
30 seconds  - для быстрого анализа
1 minute    - standard window (default)
5 minutes   - для более гладкого тренда
Custom      - пользователь задает сам
```

**Display Location:**
```
┌─────────────────────────────────────────┐
│ CPU Usage: 45% (now)                    │ ← Current
├─────────────────────────────────────────┤
│ │ Current: 45%  │ Avg 30s: 42%        │ ← Averages
│ │ Avg 1m: 43.2% │ Avg 5m: 41.8%       │
└─────────────────────────────────────────┘

Or in a separate "Stats" tab:
┌─────────────────────────────────────────┐
│ STATISTICS (Last Minute)                │
├─────────────────────────────────────────┤
│ CPU                                     │
│  Current:  45%                          │
│  Average:  43.2%                        │
│  Min:      12.5%                        │
│  Max:      92.1%                        │
│  P95:      78.4%                        │
│  P99:      88.7%                        │
│                                         │
│ RAM                                     │
│  Current:  523 MB (25.5%)               │
│  Average:  520 MB (25.4%)               │
│  ...                                    │
└─────────────────────────────────────────┘
```

**Implementation:**
```kotlin
class TimeWindowAverageCalculator {
    private val dataPoints = LinkedList<DataPoint>()
    private val maxDuration = 5 * 60 * 1000L // 5 minutes
    
    fun addDataPoint(value: Double, timestamp: Long) {
        dataPoints.add(DataPoint(value, timestamp))
        // Remove old points
        dataPoints.removeIf { it.timestamp < timestamp - maxDuration }
    }
    
    fun getAverage(windowMs: Long): Double {
        val now = System.currentTimeMillis()
        val cutoff = now - windowMs
        return dataPoints
            .filter { it.timestamp >= cutoff }
            .map { it.value }
            .average()
    }
    
    fun getPercentile(percentile: Int, windowMs: Long): Double {
        val now = System.currentTimeMillis()
        val cutoff = now - windowMs
        val values = dataPoints
            .filter { it.timestamp >= cutoff }
            .map { it.value }
            .sorted()
        
        val index = (values.size * percentile / 100.0).toInt()
        return if (index < values.size) values[index] else values.lastOrNull() ?: 0.0
    }
}
```

**Configuration:**
```
Settings → Analytics → Time Windows
┌──────────────────────────────────┐
│ Time Windows                     │
├──────────────────────────────────┤
│ ☑ Show 30s Average              │
│ ☑ Show 1m Average (default)     │
│ ☑ Show 5m Average               │
│ ☑ Show Percentiles (P95, P99)   │
└──────────────────────────────────┘
```

**Acceptance Criteria:**
- ✅ Averages вычисляются корректно
- ✅ Percentiles accurate
- ✅ Old data properly removed
- ✅ Memory efficient (circular buffer)
- ✅ No lag in calculations
- ✅ Timezone-aware timestamps

---

### 2.7 Feature 7: Comprehensive Settings UI

**Requirements:**
- Все настройки в одном месте
- Логичная группировка
- Instant preview
- Persist все значения

**Settings Structure:**
```
📊 SysMetrics Settings
│
├─ ⚙️ MONITORING
│  ├─ Update Interval [500ms - 5s]
│  ├─ Background Monitoring (toggle)
│  └─ Deep Sleep Mode (toggle)
│
├─ 🔔 NOTIFICATIONS
│  ├─ Peak Notifications Interval [30s/1m/5m]
│  ├─ Show CPU Peak (toggle)
│  ├─ Show RAM Peak (toggle)
│  ├─ Show Temperature (toggle)
│  ├─ Show Network Peak (toggle)
│  ├─ Show FPS Peak (toggle)
│  └─ Toast Duration [3s - 10s]
│
├─ 📈 CHARTS
│  ├─ Show Inline Charts (toggle)
│  ├─ Show CPU Chart (toggle)
│  ├─ Show RAM Chart (toggle)
│  ├─ Show Temperature Chart (toggle)
│  ├─ Show Network Chart (toggle)
│  ├─ Show FPS Chart (toggle)
│  └─ Chart Height [Small/Normal/Large]
│
├─ 📊 ANALYTICS
│  ├─ Time Windows
│  │  ├─ Show 30s Average (toggle)
│  │  ├─ Show 1m Average (toggle)
│  │  ├─ Show 5m Average (toggle)
│  │  └─ Show Percentiles (toggle)
│  ├─ FPS Monitoring (toggle)
│  └─ Jank Detection (toggle)
│
├─ 💾 DATA
│  ├─ Export Format [CSV/TXT/JSON]
│  ├─ Export Last [1m / 5m / 30m / All]
│  ├─ Export Data Button
│  ├─ Auto-Delete Old Data (toggle)
│  └─ Data Retention [7d / 30d / Never]
│
├─ 🎨 DISPLAY
│  ├─ Theme [Light/Dark/System]
│  ├─ Color Scheme [Default/High Contrast]
│  └─ Font Size [Small/Normal/Large]
│
└─ ℹ️ ABOUT
   ├─ Version
   ├─ Changelog
   └─ Send Feedback
```

---

## 3. АРХИТЕКТУРА

### 3.1 Data Flow

```
┌─────────────────────────────────────────┐
│         Settings Screen                 │
│   (SettingsFragment)                    │
└──────────────┬──────────────────────────┘
               │ User changes settings
               ↓
┌─────────────────────────────────────────┐
│    SettingsViewModel                    │
│   (validates & prepares)                │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│    PreferencesRepository                │
│   (saves to DataStore)                  │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│    Monitoring Service                   │
│   (reads settings & applies)            │
│   - Adjusts update interval             │
│   - Enables/disables features           │
│   - Configures exporters                │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│    Data Collection Layer                │
│   - Choreographer (for FPS)             │
│   - SystemMetrics (for CPU/RAM/Temp)    │
│   - NetworkMonitor (for traffic)        │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│    Analytics & Processing               │
│   - TimeWindowAverageCalculator         │
│   - ChartDataBuffer                     │
│   - PeakNotificationManager             │
│   - DataExporter                        │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│    UI Updates                           │
│   - Overlay (updated metrics)           │
│   - Charts (new data points)            │
│   - Stats Panel (averages)              │
│   - Notifications (toasts/alerts)       │
└─────────────────────────────────────────┘
```

### 3.2 New Entities & Models

```kotlin
// Settings
data class MonitoringSettings(
    val updateIntervalMs: Long,        // 500-5000
    val showCpuChart: Boolean,
    val showRamChart: Boolean,
    val showTempChart: Boolean,
    val showNetworkChart: Boolean,
    val showFpsChart: Boolean,
    val chartHeight: ChartHeight,      // SMALL, NORMAL, LARGE
    
    val peakNotificationInterval: Long, // 30s, 1m, 5m
    val showCpuPeak: Boolean,
    val showRamPeak: Boolean,
    val showTempPeak: Boolean,
    val showNetPeak: Boolean,
    val showFpsPeak: Boolean,
    val toastDurationMs: Int,
    
    val show30sAverage: Boolean,
    val show1mAverage: Boolean,
    val show5mAverage: Boolean,
    val showPercentiles: Boolean,
    
    val showFpsMonitoring: Boolean,
    val showJankDetection: Boolean,
    val fpsThreshold: Int              // 30 fps default
)

// Analytics
data class TimeWindowStats(
    val current: Float,
    val avg30s: Float,
    val avg1m: Float,
    val avg5m: Float,
    val min: Float,
    val max: Float,
    val p95: Float,
    val p99: Float
)

data class ChartDataPoint(
    val timestamp: Long,
    val value: Float,
    val severity: Severity             // LOW, MEDIUM, HIGH
)

// Export
enum class ExportFormat {
    CSV, TXT, JSON
}

data class ExportConfig(
    val format: ExportFormat,
    val timeRange: TimeRange,
    val resources: Set<Resource>
)

enum class TimeRange {
    LAST_1_MINUTE, LAST_5_MINUTES, LAST_30_MINUTES, LAST_1_HOUR, ALL
}
```

---

## 4. IMPLEMENTATION PHASES

### PHASE 1: Settings Infrastructure (2-3 дня)
- ✅ SettingsScreen UI
- ✅ DataStore integration
- ✅ PreferencesRepository
- ✅ All settings models

### PHASE 2: Monitoring Configuration (1-2 дня)
- ✅ Update interval control
- ✅ Feature toggles
- ✅ Instant apply without restart

### PHASE 3: Peak Notifications (2-3 дня)
- ✅ Toast implementation
- ✅ Peak tracking
- ✅ Scheduled notifications
- ✅ Per-resource toggle

### PHASE 4: Inline Charts (3-4 дня)
- ✅ Chart library integration
- ✅ Data buffer management
- ✅ Smooth rendering
- ✅ Per-chart toggle

### PHASE 5: FPS Monitoring (2-3 дня)
- ✅ Choreographer API integration
- ✅ Frame counting logic
- ✅ Jank detection
- ✅ Performance optimization

### PHASE 6: Time-Window Analytics (2-3 дня)
- ✅ Data accumulation
- ✅ Average calculations
- ✅ Percentile computation
- ✅ Memory management

### PHASE 7: Data Export & Sharing (2-3 дня)
- ✅ CSV exporter
- ✅ TXT exporter
- ✅ JSON exporter
- ✅ Share intent integration

### PHASE 8: Testing & Optimization (2-3 дня)
- ✅ Unit tests
- ✅ Integration tests
- ✅ Performance benchmarks
- ✅ Memory profiling

**TOTAL: 16-23 дня (~100-150 часов)**

---

## 5. PERFORMANCE TARGETS

| Metric | Target | Current |
|--------|--------|---------|
| Settings Load | <100ms | - |
| Chart Render | <16ms (60fps) | - |
| FPS Calculation | <1ms | - |
| Export Generation | <500ms | - |
| Memory Overhead | <50MB | - |
| CPU Overhead | <2% additional | - |

---

## 6. KEY IMPLEMENTATION DETAILS

### 6.1 Data Persistence

```kotlin
// DataStore for settings
dataStore.edit { preferences ->
    preferences[UPDATE_INTERVAL_KEY] = 1000L
    preferences[SHOW_CPU_CHART_KEY] = true
    preferences[PEAK_NOTIFICATION_INTERVAL_KEY] = 60_000L
}

// Room database for historical data
@Entity(tableName = "metrics_data")
data class MetricsDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val cpuPercent: Float,
    val ramMb: Long,
    val tempCelsius: Float,
    val fpsValue: Int,
    // ... other metrics
)
```

### 6.2 Chart Rendering

```kotlin
// MPAndroidChart или custom Canvas
class MetricsChartView(context: Context) : View(context) {
    private val dataBuffer = CircularBuffer<ChartDataPoint>(60)
    
    override fun onDraw(canvas: Canvas) {
        if (dataBuffer.isEmpty()) return
        
        val path = Path()
        dataBuffer.forEachIndexed { index, point ->
            val x = (index * width / 60f)
            val y = (1 - point.value / 100f) * height
            
            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }
        
        canvas.drawPath(path, paint)
    }
    
    fun addDataPoint(point: ChartDataPoint) {
        dataBuffer.add(point)
        invalidate()
    }
}
```

### 6.3 Toast Notifications

```kotlin
fun showPeakNotification(stats: PeakStats) {
    val text = buildString {
        append("Peak Stats (Last Minute)\n")
        append("CPU: ${stats.cpuPeak}% @ ${stats.cpuPeakTime}\n")
        append("RAM: ${stats.ramPeak}MB @ ${stats.ramPeakTime}\n")
        append("Avg: CPU ${stats.cpuAvg}% | RAM ${stats.ramAvg}MB")
    }
    
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
```

### 6.4 FPS Monitoring

```kotlin
class FpsMonitor(mainLooper: Looper) {
    private val choreographer = Choreographer.getInstance()
    private var frameCount = 0
    private var lastSecondTime = System.nanoTime()
    private val fpsFlow = MutableStateFlow(0)
    
    fun start() {
        choreographer.postFrameCallback(frameCallback)
    }
    
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            val now = System.nanoTime()
            val elapsedNs = now - lastSecondTime
            
            if (elapsedNs >= 1_000_000_000L) {
                fpsFlow.value = frameCount
                frameCount = 0
                lastSecondTime = now
            }
            
            choreographer.postFrameCallback(this)
        }
    }
}
```

---

## 7. TESTING STRATEGY

### Unit Tests
- Settings validation
- Average calculations
- Percentile computation
- Export format generation

### Integration Tests
- Settings persistence
- Chart updates
- Toast notifications
- Data export flow

### Performance Tests
- Chart rendering fps
- Memory usage
- CPU overhead
- Export time

---

## 8. SUCCESS CRITERIA

✅ All settings persist correctly  
✅ Update intervals work as expected  
✅ Notifications appear on schedule  
✅ Charts render smoothly  
✅ FPS monitoring accurate (±2fps)  
✅ Exports generate valid files  
✅ Sharing works on all platforms  
✅ <50MB additional memory  
✅ <2% additional CPU  
✅ >80% test coverage  

---

## REFERENCES

- [Android DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore)
- [Choreographer API](https://developer.android.com/reference/android/view/Choreographer)
- [Frame Rate Basics](https://developer.android.com/guide/topics/graphics/frame-rate)
- [Android Performance Best Practices](https://developer.android.com/develop/performance)
- [MPAndroidChart Library](https://github.com/PhilJay/MPAndroidChart)

---

**Ready for Claude Opus 4.5 Implementation**

