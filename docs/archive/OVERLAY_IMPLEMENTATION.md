# SysMetrics - SystemOverlay Implementation

## Overview
SysMetrics has been transformed from a standard Android TV app into a **SystemOverlay application** that displays real-time metrics as a floating window over other applications. This implementation follows the TvOverlay_cpu reference architecture.

## Architecture

### Core Components

#### 1. **SimpleOverlayService**
- Location: `app/src/main/java/com/sysmetrics/app/service/SimpleOverlayService.kt`
- Foreground service with WindowManager integration
- Handler-based periodic updates (default: 500ms)
- XML layout inflation for overlay UI
- Dynamic metric color updates based on thresholds

**Key Features:**
- Foreground notification for Android 8+
- WindowManager.LayoutParams for overlay window
- Non-focusable, non-touchable overlay
- Automatic metric refresh

#### 2. **MetricsCollector**
- Location: `app/src/main/java/com/sysmetrics/app/utils/MetricsCollector.kt`
- Wrapper around SystemDataSource for synchronous access
- CPU usage calculation with delta stats
- RAM usage in MB and percentage
- Temperature monitoring
- Core count detection

**Methods:**
```kotlin
fun getCpuUsage(): Float              // Returns 0-100
fun getRamUsage(): Triple<Long, Long, Float>  // Used, Total, Percent
fun getTemperature(): Float           // Celsius or -1 if unavailable
fun getCoreCount(): Int               // CPU core count
```

#### 3. **BootCompleteReceiver**
- Location: `app/src/main/java/com/sysmetrics/app/receiver/BootCompleteReceiver.kt`
- Auto-start overlay on device boot
- Checks preferences before starting service
- Handles multiple boot actions (standard, quickboot)

#### 4. **MainActivity (Simplified)**
- Location: `app/src/main/java/com/sysmetrics/app/ui/MainActivity.kt`
- PreferenceFragmentCompat-based UI
- Simple toggle for overlay enable/disable
- Permission request handling
- Service lifecycle management

### Layout Structure

#### overlay_metrics.xml
- **Width:** 300dp (configurable)
- **Layout:** LinearLayout with vertical orientation
- **Components:**
  - Title TextView
  - CPU metric (label, value, progress bar)
  - RAM metric (label, value, progress bar)
  - Temperature metric (label, value)
  - Footer info (core count)

#### Styling
- Dark theme optimized for Android TV
- Dynamic colors based on metric values:
  - **Green** (metric_success): < 50%
  - **Yellow** (metric_warning): 50-80%
  - **Red** (metric_error): > 80%
- 90% opacity background
- 12dp corner radius
- Progress bars with animated updates

## Configuration

### Preferences (root_preferences.xml)

| Preference Key | Type | Default | Description |
|---------------|------|---------|-------------|
| `overlay_enabled` | Switch | false | Enable/disable overlay |
| `auto_start_enabled` | Switch | false | Auto-start on boot |
| `show_cpu` | Checkbox | true | Display CPU metric |
| `show_ram` | Checkbox | true | Display RAM metric |
| `show_temperature` | Checkbox | true | Display temperature |
| `update_interval` | List | 500 | Update frequency (ms) |

### Update Intervals
- **Very Fast:** 100ms
- **Fast:** 250ms
- **Normal:** 500ms (default)
- **Slow:** 1000ms
- **Very Slow:** 2000ms

## Permissions

Required permissions in AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## Usage

### Starting the Overlay

**Via MainActivity:**
1. Open SysMetrics app
2. Toggle "Enable Overlay" switch
3. Grant overlay permission when prompted
4. Overlay appears in top-left corner

**Via Code:**
```kotlin
val serviceIntent = Intent(context, SimpleOverlayService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(serviceIntent)
} else {
    context.startService(serviceIntent)
}
```

### Stopping the Overlay

**Via MainActivity:**
1. Toggle "Enable Overlay" switch to OFF
2. Service stops and overlay disappears

**Via Code:**
```kotlin
val serviceIntent = Intent(context, SimpleOverlayService::class.java)
context.stopService(serviceIntent)
```

### Auto-Start on Boot

1. Enable "Enable Overlay" preference
2. Enable "Auto-start on Boot" preference
3. Reboot device
4. Overlay starts automatically

## Customization

### Changing Overlay Position

Edit `SimpleOverlayService.createLayoutParams()`:
```kotlin
// Top-left (default)
gravity = Gravity.TOP or Gravity.START
x = 20
y = 50

// Top-right
gravity = Gravity.TOP or Gravity.END
x = 20
y = 50

// Bottom-left
gravity = Gravity.BOTTOM or Gravity.START
x = 20
y = 50

// Bottom-right
gravity = Gravity.BOTTOM or Gravity.END
x = 20
y = 50
```

### Changing Update Interval

Modify `UPDATE_INTERVAL_MS` in `SimpleOverlayService`:
```kotlin
private const val UPDATE_INTERVAL_MS = 500L  // Change to desired interval
```

### Changing Overlay Size

Modify dimensions in `overlay_metrics.xml`:
```xml
<LinearLayout
    android:layout_width="300dp"  <!-- Change width -->
    android:layout_height="wrap_content">
```

Or in `SimpleOverlayService`:
```kotlin
companion object {
    private const val OVERLAY_WIDTH = 300  // Change to desired width
}
```

### Color Thresholds

Edit `getColorForValue()` in `SimpleOverlayService`:
```kotlin
private fun getColorForValue(percent: Float): Int {
    return when {
        percent < 50 -> getColor(R.color.metric_success)  // Green
        percent < 80 -> getColor(R.color.metric_warning)  // Yellow
        else -> getColor(R.color.metric_error)            // Red
    }
}
```

## Performance Considerations

### Memory Optimization
- Pre-allocated StringBuilder for string formatting
- View references cached during inflation
- Handler post/remove for lifecycle management
- No coroutines overhead

### CPU Optimization
- Efficient delta-based CPU calculation
- Direct /proc file reading
- Minimal allocations in update loop
- Configurable update intervals

### Battery Optimization
- Foreground service ensures reliability
- Handler-based updates (no polling threads)
- Stop service when not needed
- Adjustable update frequency

## Testing

### Manual Testing Checklist

- [ ] Overlay displays on app launch
- [ ] Metrics update in real-time
- [ ] CPU usage shows correct percentage
- [ ] RAM usage shows MB and percentage
- [ ] Temperature displays (if available)
- [ ] Toggle switch stops/starts overlay
- [ ] Overlay permission request works
- [ ] Auto-start on boot works
- [ ] Foreground notification appears
- [ ] Overlay stays on top of other apps
- [ ] Service survives app exit
- [ ] Color changes based on thresholds

### Debugging

Enable verbose logging:
```kotlin
// In SimpleOverlayService
Timber.d("Metrics updated: CPU=%.1f%% RAM=%d/%d MB", cpuPercent, usedMb, totalMb)
```

Check service status:
```bash
adb shell dumpsys activity services com.sysmetrics.app
```

View logs:
```bash
adb logcat -s SysMetrics:D SimpleOverlayService:D
```

## Troubleshooting

### Overlay Not Appearing
1. Check overlay permission granted
2. Verify service is running: `adb shell dumpsys activity services`
3. Check logcat for errors
4. Ensure WindowManager added view successfully

### Metrics Not Updating
1. Check update interval setting
2. Verify Handler is posting runnable
3. Check SystemDataSource is reading correctly
4. Look for exceptions in updateMetrics()

### Boot Auto-Start Not Working
1. Verify RECEIVE_BOOT_COMPLETED permission
2. Check BootCompleteReceiver registered in manifest
3. Enable both preferences (overlay + auto-start)
4. Test with: `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`

### Service Stops Unexpectedly
1. Ensure foreground notification is shown
2. Check battery optimization settings
3. Verify START_STICKY return value
4. Look for system killing service (low memory)

## Migration from Original Architecture

### Removed Components
- ❌ OverlayService (complex coroutine-based)
- ❌ OverlayView (custom Canvas drawing)
- ❌ MainViewModel (StateFlow-based)
- ❌ Complex UI binding

### Added Components
- ✅ SimpleOverlayService (Handler-based)
- ✅ XML layout for overlay
- ✅ MetricsCollector utility
- ✅ PreferenceFragment UI
- ✅ BootCompleteReceiver

### Benefits
- Simpler architecture
- Easier to understand and modify
- Less boilerplate code
- Better performance (no coroutines overhead)
- More maintainable

## Future Enhancements

### Possible Improvements
1. **Draggable Overlay** - Allow user to move overlay
2. **Multiple Positions** - Quick position presets
3. **Expandable View** - Show more metrics on click
4. **History Graphs** - Mini charts for trends
5. **Themes** - Light/dark/custom themes
6. **Widget Alternative** - Home screen widget
7. **Notification Controls** - Start/stop from notification
8. **Performance Profiles** - Gaming, battery saver, etc.

## Reference Architecture
This implementation follows patterns from **TvOverlay_cpu** project:
- Handler-based updates
- XML layout inflation
- WindowManager integration
- Simple foreground service
- Preference-based configuration
- Boot receiver for auto-start

## License
MIT License - See root LICENSE file

## Support
For issues or questions, create an issue on GitHub.

---
**Version:** 1.0.0  
**Last Updated:** 2025-12-10  
**Target Android:** 5.0+ (API 21+)
