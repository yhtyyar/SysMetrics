# SysMetrics - Quick Start Guide

## 🚀 Installation

1. **Build APK:**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Install on device:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📱 Usage

### First Launch

1. Open **SysMetrics** app
2. Toggle **"Enable Overlay"** switch
3. Grant overlay permission when prompted
4. Overlay appears in top-left corner showing:
   - ⚙ CPU usage (%)
   - 💾 RAM usage (MB)
   - 🌡 Temperature (°C)

### Settings

Available preferences:
- **Enable Overlay** - Start/stop overlay service
- **Auto-start on Boot** - Launch overlay automatically
- **Show CPU/RAM/Temperature** - Toggle specific metrics
- **Update Interval** - Change refresh rate (100ms-2000ms)

### Stop Overlay

Toggle **"Enable Overlay"** switch to OFF

## 🎯 Key Features

✅ **Real-time metrics** - Updates every 500ms  
✅ **System overlay** - Runs over other apps  
✅ **Color-coded** - Green/Yellow/Red based on usage  
✅ **Auto-start** - Optional boot receiver  
✅ **Low overhead** - Minimal CPU/battery impact  
✅ **Android TV optimized** - D-pad navigation support  

## 📊 Overlay Display

```
┌─────────────────────┐
│ SysMetrics          │
│                     │
│ ⚙ CPU               │
│ 48.5%    [====]     │
│                     │
│ 💾 RAM              │
│ 1250/1699 MB [===]  │
│                     │
│ 🌡 Temp             │
│ 45°C                │
│                     │
│ Cores: 4            │
└─────────────────────┘
```

## 🔧 Troubleshooting

**Overlay not showing?**
- Check overlay permission granted
- Verify service running: `adb shell dumpsys activity services`
- Check logs: `adb logcat -s SysMetrics:D`

**Metrics not updating?**
- Check update interval setting
- Verify SystemDataSource working
- Look for errors in logcat

**Auto-start not working?**
- Enable both "Enable Overlay" AND "Auto-start on Boot"
- Check RECEIVE_BOOT_COMPLETED permission
- Test: `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`

## 📁 Project Structure

```
app/src/main/java/com/sysmetrics/app/
├── service/
│   ├── SimpleOverlayService.kt    # Main overlay service
│   └── OverlayService.kt          # Legacy service
├── receiver/
│   └── BootCompleteReceiver.kt    # Auto-start on boot
├── utils/
│   └── MetricsCollector.kt        # Metrics wrapper
└── ui/
    └── MainActivity.kt             # Settings UI

app/src/main/res/
├── layout/
│   ├── overlay_metrics.xml        # Overlay UI layout
│   └── activity_main_simple.xml   # Main activity
└── xml/
    └── root_preferences.xml        # Settings
```

## 🎨 Customization

### Change Position

Edit `SimpleOverlayService.kt`:
```kotlin
// Top-right
gravity = Gravity.TOP or Gravity.END
x = 20
y = 50
```

### Change Colors

Edit `colors.xml`:
```xml
<color name="metric_success">#4CAF50</color>  <!-- Green -->
<color name="metric_warning">#FFC107</color>  <!-- Yellow -->
<color name="metric_error">#F44336</color>    <!-- Red -->
```

### Change Update Rate

Edit `SimpleOverlayService.kt`:
```kotlin
private const val UPDATE_INTERVAL_MS = 500L  // milliseconds
```

## 📖 Documentation

Full documentation: `OVERLAY_IMPLEMENTATION.md`

## 🐛 Debug Commands

```bash
# View running services
adb shell dumpsys activity services com.sysmetrics.app

# Monitor logs
adb logcat -s SysMetrics:D SimpleOverlayService:D MetricsCollector:D

# Check overlay permission
adb shell appops get com.sysmetrics.app SYSTEM_ALERT_WINDOW

# Test boot receiver
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED \
  -p com.sysmetrics.app

# Kill service
adb shell am force-stop com.sysmetrics.app
```

## ⚡ Performance Tips

1. **Battery Optimization:**
   - Disable battery optimization for SysMetrics
   - Settings → Apps → SysMetrics → Battery → Unrestricted

2. **Update Interval:**
   - Use 1000ms or 2000ms for better battery life
   - Use 100ms or 250ms for gaming/monitoring

3. **Metrics Selection:**
   - Disable unused metrics (CPU/RAM/Temp)
   - Reduces processing overhead

## 📝 Version

- **Version:** 1.0.0
- **Build Date:** 2025-12-10
- **Min Android:** 5.0 (API 21)
- **Target Android:** 14 (API 34)

---

For detailed information, see `OVERLAY_IMPLEMENTATION.md`
