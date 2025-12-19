# 🔧 Network Traffic Monitoring - Troubleshooting Guide

## Common Issues and Solutions

### 1. Native Library Not Loading

**Symptoms:**
- "Native: OFF" in UI
- Kotlin fallback being used
- Slower performance

**Possible Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Library not compiled | Run `./gradlew assembleDebug` to rebuild |
| Wrong ABI | Check device architecture matches ndk.abiFilters |
| ProGuard stripping | Add keep rules for JNI methods |
| Missing NDK | Install NDK 25.2.9519653 via SDK Manager |

**Verification:**
```kotlin
val isNative = NativeNetworkMetrics().isNativeAvailable()
Timber.d("Native available: $isNative")
```

**ProGuard Rules:**
```proguard
-keep class com.sysmetrics.app.native_bridge.NativeNetworkMetrics {
    native <methods>;
}
```

---

### 2. Zero Traffic Reading

**Symptoms:**
- Always showing ↓0 | ↑0
- No speed updates

**Possible Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| /proc/net/dev not accessible | Check file permissions |
| All interfaces filtered | Verify non-loopback interfaces exist |
| Baseline not initialized | Wait for second reading |
| Time delta too small | Ensure >50ms between readings |

**Debugging:**
```bash
# On device via adb shell
cat /proc/net/dev

# Check permissions
ls -la /proc/net/dev
```

---

### 3. Inaccurate Speed Readings

**Symptoms:**
- Values differ significantly from speedtest
- Inconsistent readings

**Possible Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Wrong time delta | Check system clock stability |
| Counter overflow | Handle 64-bit counter wrap |
| Multiple interfaces | Verify aggregation logic |
| Cached values | Reset baseline |

**Accuracy Verification:**
```bash
# Compare with ifconfig
adb shell ifconfig wlan0

# Run for 10 seconds, compare delta
```

**Expected Accuracy:** ±5% of actual traffic

---

### 4. Per-App Stats Not Showing

**Symptoms:**
- Empty per-app list
- Only system-wide stats visible

**Possible Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| xt_qtaguid unavailable | Fallback to TrafficStats API |
| Permission denied | Request PACKAGE_USAGE_STATS |
| No recent app traffic | Apps must have active traffic |
| Cache stale | Call `clearCache()` |

**Check xt_qtaguid:**
```bash
adb shell cat /proc/net/xt_qtaguid/stats
# If "Permission denied" - not available without root
```

**Permission Request:**
```kotlin
if (!hasUsageStatsPermission()) {
    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
}
```

---

### 5. Network Type Detection Wrong

**Symptoms:**
- WiFi showing as LTE
- Wrong carrier name
- Missing signal strength

**Possible Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Missing permissions | Add READ_PHONE_STATE to manifest |
| API level issues | Check Android version handling |
| VPN active | VPN may mask underlying type |
| Airplane mode | Returns NONE correctly |

**Required Permissions:**
```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

---

### 6. High CPU/Memory Usage

**Symptoms:**
- CPU usage >1%
- Memory growing over time
- Battery drain

**Possible Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Update interval too fast | Increase to 1000ms+ |
| Native not available | Enable native for 10x speedup |
| Memory leaks | Check icon cache, clear periodically |
| Too many Flow collectors | Verify lifecycle handling |

**Profiling:**
```bash
# CPU profiling
adb shell top -m 10 | grep sysmetrics

# Memory check
adb shell dumpsys meminfo com.sysmetrics.app
```

**Recommended Settings:**
- Update interval: 1000ms (normal), 2000ms (battery saver)
- Max cached apps: 50
- Cache cleanup interval: 5 minutes

---

### 7. Alerts Not Triggering

**Symptoms:**
- High speed but no alert
- Quota exceeded but no warning

**Possible Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Alerts disabled | Check `alertConfig.enabled` |
| Threshold too high | Lower threshold in settings |
| Flow not collected | Verify `observeAlerts()` collected |
| Config not persisted | Check DataStore write |

**Debug Alerts:**
```kotlin
monitorUseCase.observeAlerts()
    .onEach { alert ->
        Timber.d("Alert: ${alert.type} - ${alert.message}")
    }
    .launchIn(scope)
```

---

### 8. Overlay Not Updating

**Symptoms:**
- Frozen values
- Overlay showing stale data

**Possible Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Service stopped | Check overlay service lifecycle |
| UI thread blocked | Move computation off main thread |
| View not invalidated | Call `invalidate()` after update |
| Flow cancelled | Check coroutine scope |

**Overlay Service Check:**
```kotlin
// In service
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.d("Overlay service started")
    return START_STICKY
}
```

---

### 9. Build/Compilation Errors

**Native Build Errors:**

| Error | Solution |
|-------|----------|
| CMake not found | Install CMake via SDK Manager |
| NDK not found | Set ndkVersion in build.gradle.kts |
| JNI method not found | Verify method signature matches |
| Linker error | Check library dependencies |

**CMake Debug:**
```bash
./gradlew :app:externalNativeBuildDebug --info
```

**JNI Signature Verification:**
```bash
# Generate correct signatures
javap -s -p com.sysmetrics.app.native_bridge.NativeNetworkMetrics
```

---

### 10. Test Failures

**Unit Test Issues:**

| Error | Solution |
|-------|----------|
| /proc/net/dev not found | Mock file reading in tests |
| Context null | Use ApplicationProvider |
| Flow timeout | Increase Turbine timeout |
| MockK errors | Verify mock setup |

**Test Configuration:**
```kotlin
@Before
fun setup() {
    Dispatchers.setMain(testDispatcher)
    // Setup mocks
}

@After
fun tearDown() {
    Dispatchers.resetMain()
}
```

---

## Diagnostic Commands

### Device Information
```bash
# Android version
adb shell getprop ro.build.version.release

# Architecture
adb shell getprop ro.product.cpu.abi

# Network interfaces
adb shell ip link show
```

### Network Status
```bash
# Current connections
adb shell netstat -an

# Traffic stats
adb shell cat /proc/net/dev

# Per-UID stats (if available)
adb shell cat /proc/net/xt_qtaguid/stats
```

### App Diagnostics
```bash
# App memory
adb shell dumpsys meminfo com.sysmetrics.app

# App CPU
adb shell top -n 1 | grep sysmetrics

# Logcat filter
adb logcat -s NET_STATS_DS:V NET_TYPE_DETECT:V NATIVE_NET_METRICS:V
```

---

## Log Tags Reference

| Tag | Component | Level |
|-----|-----------|-------|
| NET_STATS_DS | NetworkStatsDataSource | V,D,W,E |
| NET_TYPE_DETECT | NetworkTypeDetector | V,D,W,E |
| PERAPP_TRAFFIC_DS | PerAppTrafficDataSource | V,D,W,E |
| NATIVE_NET_METRICS | NativeNetworkMetrics | D,W,E |
| NET_STATS_REPO | NetworkStatsRepository | D,E |
| GET_NET_STATS_UC | GetNetworkStatsUseCase | D,E |
| MONITOR_NET_UC | MonitorNetworkTrafficUseCase | V,D,W,E |
| NET_STATS_VM | NetworkStatsViewModel | D,E |
| NET_STATS_FRAG | NetworkStatsFragment | D |
| NET_OVERLAY_VIEW | NetworkOverlayView | D |

---

## Performance Benchmarks

### Expected Performance

| Metric | Native | Kotlin | Target |
|--------|--------|--------|--------|
| Parse time | <10ms | <50ms | <100ms |
| CPU usage | <0.5% | <1% | <1% |
| Memory | <10MB | <15MB | <20MB |

### Running Benchmarks
```bash
./gradlew :app:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
  com.sysmetrics.app.benchmark.NetworkStatsBenchmarkTest
```

---

## Getting Help

1. **Check logs**: `adb logcat | grep -E "NET_|NATIVE_"`
2. **Enable verbose**: Set Timber to VERBOSE level
3. **Run tests**: `./gradlew test` for unit tests
4. **File issue**: Include device info, Android version, logs

---

*Troubleshooting guide for SysMetrics Pro Network Traffic Monitoring*
*Version 1.0 - December 2025*
