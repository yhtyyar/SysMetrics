# 📝 Network Traffic Monitoring - Implementation Notes

## Overview

This document contains implementation details, trade-offs, and developer notes for the Network Traffic Monitoring feature in SysMetrics Pro.

## File Structure

```
app/src/main/java/com/sysmetrics/app/
├── data/
│   ├── model/network/
│   │   └── NetworkTrafficStats.kt      # Data classes & enums
│   ├── repository/
│   │   └── NetworkStatsRepository.kt   # Repository implementation
│   └── source/network/
│       ├── NetworkStatsDataSource.kt   # /proc/net/dev parsing
│       ├── NetworkTypeDetector.kt      # ConnectivityManager wrapper
│       └── PerAppTrafficDataSource.kt  # Per-UID traffic
├── domain/
│   ├── repository/
│   │   └── INetworkStatsRepository.kt  # Repository interface
│   └── usecase/network/
│       ├── GetNetworkStatsUseCase.kt   # Snapshot retrieval
│       └── MonitorNetworkTrafficUseCase.kt  # Continuous monitoring
├── native_bridge/
│   └── NativeNetworkMetrics.kt         # JNI bridge
├── core/di/
│   └── NetworkModule.kt                # Manual DI
└── ui/network/
    ├── NetworkStatsFragment.kt         # Detail screen
    ├── NetworkStatsViewModel.kt        # MVVM ViewModel
    ├── NetworkOverlayView.kt           # Custom overlay view
    └── NetworkSettingsFragment.kt      # Preferences

app/src/main/cpp/
├── CMakeLists.txt                      # NDK build config
├── native_network_stats.h              # C++ header
└── native_network_stats.cpp            # C++ implementation

app/src/main/res/
├── layout/
│   ├── fragment_network_stats.xml
│   └── item_per_app_traffic.xml
└── xml/
    └── network_preferences.xml

app/src/test/java/.../
├── NetworkStatsDataSourceTest.kt
├── NetworkTypeDetectorTest.kt
└── NetworkStatsRepositoryTest.kt

app/src/androidTest/java/.../
└── NetworkStatsBenchmarkTest.kt
```

## /proc/net/dev Parsing

### Format

```
Inter-|   Receive                                                |  Transmit
 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
    lo: 1234567   12345    0    0    0     0          0         0  1234567   12345    0    0    0     0       0          0
  eth0: 98765432  654321    0    0    0     0          0         0  87654321  123456    0    0    0     0       0          0
 wlan0: 55555555  333333    0    0    0     0          0         0  44444444  222222    0    0    0     0       0          0
```

### Parsing Logic

1. Skip first 2 header lines
2. Split on colon to get interface name
3. Parse space-separated values after colon
4. Skip loopback interface (`lo`)
5. Sum all interface bytes for total

### Key Indices

| Index | Field | Description |
|-------|-------|-------------|
| 0 | rx_bytes | Received bytes |
| 1 | rx_packets | Received packets |
| 2 | rx_errs | Receive errors |
| 3 | rx_drop | Dropped received |
| 8 | tx_bytes | Transmitted bytes |
| 9 | tx_packets | Transmitted packets |
| 10 | tx_errs | Transmit errors |
| 11 | tx_drop | Dropped transmitted |

## Delta Calculation

```kotlin
// Snapshot pattern
val timeDeltaSec = (currentTime - previousTime) / 1000f
val rxDelta = (currentRxBytes - previousRxBytes).coerceAtLeast(0L)
val txDelta = (currentTxBytes - previousTxBytes).coerceAtLeast(0L)

val ingressBytesPerSec = (rxDelta / timeDeltaSec).toLong()
val egressBytesPerSec = (txDelta / timeDeltaSec).toLong()

// Convert to Mbps: bytes/sec * 8 bits/byte / 1024 / 1024
val ingressMbps = ingressBytesPerSec * 8f / (1024f * 1024f)
```

## Unit Conversions

| From | To | Formula |
|------|----|---------|
| Bytes/sec | KB/s | bytes / 1024 |
| Bytes/sec | MB/s | bytes / 1024 / 1024 |
| Bytes/sec | Mbps | bytes * 8 / 1024 / 1024 |
| Mbps | Bytes/sec | mbps * 1024 * 1024 / 8 |

## Native C++ Implementation

### Stack vs Heap Allocation

```cpp
// GOOD: Stack allocation (fast, no GC)
InterfaceStatsNative interfaces[MAX_INTERFACES];
char buffer[READ_BUFFER_SIZE];

// AVOID: Heap allocation
// InterfaceStatsNative* interfaces = new InterfaceStatsNative[MAX_INTERFACES];
```

### File Reading Optimization

```cpp
// Open file once, read entire content
int fd = open(PROC_NET_DEV, O_RDONLY);
ssize_t bytes_read = read(fd, buffer, sizeof(buffer) - 1);
close(fd);

// Parse in-memory buffer
char* line = strtok(buffer, "\n");
```

### JNI Array Return

```cpp
// Return array: [rx_bytes, tx_bytes, timestamp]
jlongArray result = (*env)->NewLongArray(env, 3);
jlong data[3] = { (jlong)rx_bytes, (jlong)tx_bytes, (jlong)timestamp };
(*env)->SetLongArrayRegion(env, result, 0, 3, data);
return result;
```

## Network Type Detection

### Transport Priority

```kotlin
when {
    capabilities.hasTransport(TRANSPORT_WIFI) -> WIFI
    capabilities.hasTransport(TRANSPORT_CELLULAR) -> detectCellularGeneration()
    capabilities.hasTransport(TRANSPORT_ETHERNET) -> ETHERNET
    capabilities.hasTransport(TRANSPORT_VPN) -> VPN
    else -> UNKNOWN
}
```

### Cellular Generation Detection

| Network Type Constant | Generation |
|-----------------------|------------|
| NETWORK_TYPE_NR | 5G |
| NETWORK_TYPE_LTE, IWLAN | 4G LTE |
| NETWORK_TYPE_UMTS, HSDPA, HSPA, etc. | 3G |
| NETWORK_TYPE_GPRS, EDGE, CDMA | 2G |

## Per-App Traffic

### Data Sources

1. **Primary**: `/proc/net/xt_qtaguid/stats` (requires elevated permissions)
2. **Fallback**: `TrafficStats.getUidRxBytes(uid)` / `getUidTxBytes(uid)`

### xt_qtaguid Format

```
idx iface acct_tag_hex uid_tag_int cnt_set rx_bytes rx_packets tx_bytes tx_packets ...
2 wlan0 0x0 10045 0 123456 1234 654321 4321 ...
```

### App Info Caching

```kotlin
private val appInfoCache = ConcurrentHashMap<Int, CachedAppInfo>()

data class CachedAppInfo(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val lastAccessed: Long
)
```

## Peak Value Tracking

```kotlin
@Volatile
private var peakIngressMbps: Float = 0f

@Volatile
private var peakEgressMbps: Float = 0f

private fun updatePeakValues(ingressMbps: Float, egressMbps: Float, timestamp: Long) {
    if (ingressMbps > peakIngressMbps) {
        peakIngressMbps = ingressMbps
        peakIngressTimestamp = timestamp
    }
    if (egressMbps > peakEgressMbps) {
        peakEgressMbps = egressMbps
        peakEgressTimestamp = timestamp
    }
}
```

## Alert System

### Configuration Storage

```kotlin
// DataStore preferences
private val KEY_ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
private val KEY_HIGH_SPEED_THRESHOLD = floatPreferencesKey("high_speed_threshold")
private val KEY_DAILY_QUOTA_MB = longPreferencesKey("daily_quota_mb")
```

### Alert Types

| Type | Condition | Default Threshold |
|------|-----------|-------------------|
| HIGH_SPEED | speed > threshold | 100 Mbps |
| QUOTA_WARNING | usage > quota * warning% | 80% |
| QUOTA_EXCEEDED | usage > quota | 100% |
| ANOMALY_DETECTED | unusual pattern | ML-based (future) |

## Display Modes

### Compact

```
↓ 2.5M | ↑ 0.8M
```

### Extended

```
↓ Ingress: 2.5 Mbps | Peak: 25.3 Mbps
↑ Egress:  0.8 Mbps | Peak: 12.1 Mbps
WiFi: -45dBm
```

### Per-App

```
YouTube  ↓1.5M↑50K
Telegram ↓200K↑100K
Chrome   ↓800K↑150K
```

### Combined

```
CPU: 45% | RAM: 52% | Temp: 38°C
↓ 2.5M | ↑ 0.8M | WiFi: -45dBm
```

## Error Handling

### Graceful Degradation

```kotlin
try {
    if (useNative) {
        nativeNetworkMetrics.getNetworkStats()
    } else {
        networkStatsDataSource.readNetworkStats()
    }
} catch (e: Exception) {
    Timber.tag(TAG).e(e, "Error getting network stats")
    NetworkTrafficStats.EMPTY  // Return safe default
}
```

### Permission Handling

```kotlin
try {
    telephonyManager?.dataNetworkType
} catch (e: SecurityException) {
    Timber.tag(TAG).w(e, "No permission to read phone state")
    NetworkTypeEnum.LTE  // Default assumption
}
```

## Memory Considerations

### Icon Caching

- Use `WeakReference` or limit cache size
- Clear stale entries periodically
- Don't hold Activity context

### Flow Collection

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        // Automatically cancels when stopped
        viewModel.uiState.collectLatest { state ->
            updateUi(state)
        }
    }
}
```

## Testing Notes

### Mocking /proc/net/dev

```kotlin
// In unit tests, file won't exist
// Test parsing logic separately with mock data
val mockData = """
Inter-|   Receive                                                |  Transmit
 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
 wlan0: 1000000   10000    0    0    0     0          0         0   500000    5000    0    0    0     0       0          0
""".trimIndent()
```

### Testing Flows

```kotlin
repository.observeNetworkType().test(timeout = 5.seconds) {
    assertEquals(NetworkTypeEnum.WIFI, awaitItem().type)
    assertEquals(NetworkTypeEnum.LTE, awaitItem().type)
    awaitComplete()
}
```

## Performance Tuning

### Update Interval Recommendations

| Use Case | Interval | Rationale |
|----------|----------|-----------|
| Real-time monitoring | 500ms | Native available |
| Normal usage | 1000ms | Balance accuracy/battery |
| Background | 2000ms | Minimize impact |

### Battery Optimization

- Increase interval when screen off
- Stop monitoring when app backgrounded (configurable)
- Use JobScheduler for periodic summary

## Known Limitations

1. **Per-app stats on non-root**: Limited to TrafficStats API accuracy
2. **VPN traffic**: May show under VPN app, not original app
3. **Very high speeds**: UI may need Gbps display
4. **Old Android versions**: Some APIs unavailable on API 21-23

## Future Improvements

- [ ] Historical data storage (Room)
- [ ] Widget support
- [ ] Export to CSV/JSON
- [ ] Anomaly detection ML model
- [ ] Network quality scoring
- [ ] Bandwidth test integration

---

*Implementation notes for SysMetrics Pro Network Traffic Monitoring*
*Version 1.0 - December 2025*
