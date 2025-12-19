# 📋 ТЕХНИЧЕСКОЕ ЗАДАНИЕ (ТЗ)

## Network Traffic Monitoring & Analytics Feature для SysMetrics Pro

**Версия:** 1.0  
**Дата:** December 19, 2025  
**Статус:** Design Phase  
**Приоритет:** High  
**Сложность:** Medium-High

---

## 1. ОБЗОР И ЦЕЛЬ

### 1.1 Основная Цель
Добавить полнофункциональный мониторинг сетевого трафика в SysMetrics Pro с отслеживанием:
- **Входящего трафика (Ingress)** - загрузка данных
- **Исходящего трафика (Egress)** - отправка данных
- **Максимального использования** (Peak usage)
- **Текущего использования** (Real-time)
- **Per-app трафика** (какое приложение сколько ест)
- **Сравнение с другими метриками** (CPU, RAM, Network одновременно)

### 1.2 Контекст
SysMetrics Pro является production-ready tool для мониторинга производительности. Network Traffic Monitoring расширит его функциональность для полного анализа системных ресурсов, включая сетевую активность.

### 1.3 Бизнес-ценность
- Помощь разработчикам в отладке network-related issues
- Выявление утечек трафика (background сервисы, которые едят данные)
- Оптимизация приложений с высоким использованием сети
- Помощь QA командам при тестировании
- Ценная информация для пользователей о потреблении данных

---

## 2. ТРЕБОВАНИЯ

### 2.1 Функциональные Требования

#### 2.1.1 Real-Time Traffic Monitoring
**Требование:** Мониторить текущее сетевое использование в реальном времени

**Детали:**
- Показывать текущий Ingress (↓) в Mbps/kbps
- Показывать текущий Egress (↑) в Mbps/kbps
- Обновление каждые 500ms / 1s / 2s (настраивается)
- Точность: ±5% от реального значения
- Округление: до 2 знаков после запятой

**Примеры отображения:**
```
↓ 2.5 Mbps (Ingress)
↑ 0.8 Mbps (Egress)
```

**Источник данных:** `/proc/net/dev` (Linux стандарт)

---

#### 2.1.2 Peak Usage Tracking
**Требование:** Отслеживать максимальное использование за сессию

**Детали:**
- Хранить Peak Ingress с timestamp'ом
- Хранить Peak Egress с timestamp'ом
- Reset при перезапуске приложения
- Опционально: сохранять в SharedPreferences для persistence

**Примеры:**
```
Peak ↓: 25.3 Mbps (14:23:45)
Peak ↑: 12.1 Mbps (14:22:10)
```

---

#### 2.1.3 Per-App Traffic Analysis
**Требование:** Показывать какое приложение сколько трафика использует

**Детали:**
- Top 5 приложений по трафику (опционально Top 10)
- Показывать имя приложения + icon
- Total bytes sent/received за сессию
- Текущее использование для каждого app

**Примеры:**
```
🎬 YouTube - ↓ 1.5MB/s (↑ 50KB/s)
📱 Telegram - ↓ 200KB/s (↑ 100KB/s)
🌐 Chrome - ↓ 800KB/s (↑ 150KB/s)
```

**Способ реализации:**
- Парсинг `/proc/net/xt_qtaguid/stats` (если доступен)
- Fallback на `/proc/[pid]/net/dev` для каждого процесса
- Требует elevated permissions (может быть система-уровневая)

---

#### 2.1.4 Network Type Detection
**Требование:** Определять тип сетевого соединения

**Детали:**
- Тип: WiFi / LTE / 5G / Ethernet / None
- Имя сети (для WiFi - SSID)
- Сигнал (для LTE/5G - dBm, для WiFi - RSSI)

**Примеры:**
```
WiFi: "LimeHD-Office" (-45 dBm)
LTE: "Megafon" (-110 dBm)
5G: "Beeline-5G" (-95 dBm)
```

---

#### 2.1.5 Traffic Alerts
**Требование:** Оповещать пользователя при высоком трафике

**Опции:**
- Alert при превышении X Mbps
- Alert при использовании >Y% daily quota
- Alert при anomaly detection (трафик выше нормы)
- Можно отключить в настройках

---

#### 2.1.6 Overlay Display Modes
**Требование:** Несколько режимов отображения в overlay

**Режимы:**
1. **Compact** (текущий режим):
   ```
   ↓ 2.5M | ↑ 0.8M
   ```

2. **Extended**:
   ```
   ↓ Ingress: 2.5 Mbps | Peak: 25.3 Mbps
   ↑ Egress:  0.8 Mbps | Peak: 12.1 Mbps
   ```

3. **Per-App View**:
   ```
   🎬 YT: ↓1.5M ↑50K
   📱 TG: ↓200K ↑100K
   🌐 CH: ↓800K ↑150K
   ```

4. **Combined** (со всеми метриками):
   ```
   CPU: 45% | RAM: 52% | Temp: 38°C
   ↓ 2.5M | ↑ 0.8M | WiFi: -45dBm
   ```

---

### 2.2 Non-Functional Requirements

#### 2.2.1 Performance
- **CPU overhead**: < 1% (дополнительный к текущему 2%)
- **Memory overhead**: < 20MB дополнительно
- **Battery impact**: < 0.5% за 24 часа на фоне
- **Parsing time**: < 100ms per cycle (native C++ optimization)

#### 2.2.2 Accuracy
- ±5% от реального трафика (baseline из `ifconfig`)
- Совпадение с `adb shell ifconfig` на 95%+

#### 2.2.3 Compatibility
- Android 5.0+ (API 21-34)
- Все архитектуры (ARM32, ARM64, x86, x86_64)
- Graceful degradation если `/proc/net/dev` недоступен

#### 2.2.4 Reliability
- Не должно вызывать крашей
- Должно обрабатывать ошибки gracefully
- Fallback если нет доступа к системным файлам

#### 2.2.5 Testability
- Unit tests для парсинга
- Integration tests на реальном устройстве
- Benchmark тесты для performance

---

## 3. АРХИТЕКТУРА

### 3.1 Общая структура
```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│   OverlayView (Network Tab)             │
│   NetworkStatsFragment                  │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           Domain Layer                  │
│   GetNetworkStatsUseCase                │
│   MonitorNetworkTrafficUseCase          │
│   DetectNetworkTypeUseCase              │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            Data Layer                   │
│   NetworkStatsRepository                │
│   NetworkConnectivityRepository         │
│   PerAppTrafficRepository               │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│        Data Sources                     │
│   NetworkStatsDataSource (Kotlin)       │
│   NativeNetworkStats (C++)              │
│   ConnectivityDataSource                │
│   /proc/net/dev parsing                 │
│   /proc/net/xt_qtaguid/stats parsing    │
└─────────────────────────────────────────┘
```

### 3.2 Data Flow
```
1. NetworkStatsFlow triggered (every 1s)
   ↓
2. ParsingThread reads /proc/net/dev (C++)
   ↓
3. Calculate delta (bytes_now - bytes_prev)
   ↓
4. Convert to Mbps/kbps
   ↓
5. Update Peak values if needed
   ↓
6. Emit to ViewModel via Flow
   ↓
7. Update UI (overlay + stats screen)
```

### 3.3 Entities & Models

```kotlin
data class NetworkStats(
    val ingressBytesPerSec: Long,      // bytes per second
    val egressBytesPerSec: Long,       // bytes per second
    val ingressMbps: Float,            // display format
    val egressMbps: Float,             // display format
    val peakIngress: Float,            // peak so far
    val peakEgress: Float,             // peak so far
    val peakIngressTime: Long,         // timestamp
    val peakEgressTime: Long,          // timestamp
    val totalIngressBytes: Long,       // cumulative
    val totalEgressBytes: Long,        // cumulative
    val timestamp: Long
)

data class NetworkType(
    val type: NetworkTypeEnum,         // WiFi, LTE, 5G, Ethernet, None
    val displayName: String,           // "WiFi", "LTE", "5G"
    val networkName: String?,          // SSID for WiFi, carrier for LTE
    val signalStrength: Int?           // dBm or RSSI
)

data class PerAppTrafficStats(
    val packageName: String,
    val appName: String,
    val appIcon: Bitmap?,
    val ingressBytesPerSec: Long,
    val egressBytesPerSec: Long,
    val totalIngressBytes: Long,
    val totalEgressBytes: Long
)

enum class NetworkTypeEnum {
    WIFI, LTE, FIVE_G, ETHERNET, NONE
}
```

### 3.4 Key Classes

**NetworkStatsDataSource** (Kotlin):
```kotlin
class NetworkStatsDataSource {
    suspend fun getCurrentNetworkStats(): NetworkStats
    suspend fun getPerAppStats(): List<PerAppTrafficStats>
    fun observeNetworkStats(): Flow<NetworkStats>
}
```

**NativeNetworkStats** (C++):
```cpp
struct NetworkStatsNative {
    unsigned long ingressBytes;
    unsigned long egressBytes;
    unsigned long timestamp;
};

// JNI bridge
NetworkStatsNative getNativeNetworkStats();
```

**NetworkTypeDetector**:
```kotlin
class NetworkTypeDetector(context: Context) {
    fun getCurrentNetworkType(): Flow<NetworkType>
}
```

---

## 4. ТЕХНИЧЕСКИЙ СТЕК

| Компонент | Технология | Почему |
|-----------|-----------|--------|
| **Парсинг** | C++ via JNI | 10x быстрее, нет GC паузе |
| **Async** | Kotlin Coroutines + Flow | Reactive, non-blocking |
| **DI** | Hilt | Type-safe, compile-time checks |
| **Connectivity** | ConnectivityManager API | Official Android API |
| **Storage** | DataStore | Для сохранения peak stats |
| **Testing** | JUnit4, Mockito, Turbine | Полное покрытие |

---

## 5. РЕАЛИЗАЦИЯ (ФАЗЫ)

### ФАЗА 1: Core Traffic Monitoring (неделя 1-2)
**Deliverables:**
- ✅ NetworkStatsDataSource (Kotlin парсинг /proc/net/dev)
- ✅ Native C++ реализация
- ✅ JNI bridge
- ✅ Real-time ingress/egress мониторинг
- ✅ Peak tracking
- ✅ Unit тесты

**Time:** 10-15 часов

---

### ФАЗА 2: Network Type Detection (неделя 2)
**Deliverables:**
- ✅ NetworkTypeDetector
- ✅ ConnectivityManager интеграция
- ✅ WiFi SSID detection
- ✅ Signal strength parsing
- ✅ Тесты на разных типах сетей

**Time:** 5-8 часов

---

### ФАЗА 3: Per-App Traffic Analysis (неделя 3)
**Deliverables:**
- ✅ PerAppTrafficRepository
- ✅ /proc/net/xt_qtaguid parsing
- ✅ Top 5 apps мониторинг
- ✅ Per-app icon + name resolution
- ✅ Интеграция с overlay

**Time:** 10-15 часов

---

### ФАЗА 4: UI & Overlay Integration (неделя 3-4)
**Deliverables:**
- ✅ Overlay display modes
- ✅ Network stats fragment
- ✅ Settings для alert'ов
- ✅ Combined metrics view
- ✅ UI тесты

**Time:** 12-18 часов

---

### ФАЗА 5: Advanced Features (неделя 4-5)
**Deliverables:**
- ✅ Traffic alerts
- ✅ Anomaly detection
- ✅ Historical data (24-hour)
- ✅ CSV export
- ✅ Performance optimization

**Time:** 15-20 часов

---

### ФАЗА 6: Testing & Documentation (неделя 5)
**Deliverables:**
- ✅ Full test coverage (>80%)
- ✅ Performance benchmarks
- ✅ Documentation
- ✅ Code review
- ✅ Release prep

**Time:** 10-12 часов

**ИТОГО:** 4-5 недель, ~60-80 часов разработки

---

## 6. ИСТОЧНИКИ ДАННЫХ

### 6.1 /proc/net/dev (System-wide Traffic)
```
# Читаем каждый интерфейс (eth0, wlan0, etc.)
# Получаем:
# - Received bytes
# - Transmitted bytes
# - Packets, errors, dropped packets

cat /proc/net/dev

# Output:
# Inter-|   Receive                                                |  Transmit
#  face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
#    lo: 1234567   12345    0    0    0     0          0         0  1234567   12345    0    0    0     0       0          0
#  eth0: 98765432  654321    0    0    0     0          0         0  87654321  123456    0    0    0     0       0          0
```

### 6.2 /proc/net/xt_qtaguid/stats (Per-App Traffic)
```
# Требует elevated permissions (system app или rooted device)
# Показывает трафик для каждого UID

cat /proc/net/xt_qtaguid/stats

# Output:
# xtables
# idx iface acct_tag_hex uid_tag_int acct_obj rx_bytes rx_packets tx_bytes tx_packets rx_tcp_packets rx_tcp_bytes rx_udp_packets rx_udp_bytes ...
```

### 6.3 ConnectivityManager API
```kotlin
val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
val network = connectivityManager.activeNetwork
val capabilities = connectivityManager.getNetworkCapabilities(network)

// Определяем тип
when {
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE/5G"
}
```

---

## 7. ОБРАБОТКА ОШИБОК И EDGE CASES

| Сценарий | Решение |
|----------|---------|
| `/proc/net/dev` недоступен | Fallback на ConnectivityManager (менее точно) |
| Нет интернета | Показывать 0 Mbps, но продолжать мониторить |
| Per-app stats недоступны | Показывать only system-wide stats |
| Permission denied | Graceful degradation, логирование |
| Очень высокий трафик (>1Gbps) | Автоматический switch на Gbps единицы |
| Очень низкий трафик (<1Kbps) | Показывать в Kbps, не допускать 0 |

---

## 8. PERMISSIONS

```xml
<!-- AndroidManifest.xml -->

<!-- Для мониторинга -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" /> <!-- For signal strength -->

<!-- Для per-app stats (опционально) -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />

<!-- Для alerts -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## 9. PERFORMANCE TARGETS

| Метрика | Target | Как измерять |
|---------|--------|---|
| CPU Overhead | < 1% | `adb shell top` во время мониторинга |
| Memory Overhead | < 20MB | LeakCanary + Android Studio Profiler |
| Parsing Time | < 100ms | Benchmark тесты |
| Battery Impact (bg) | < 0.5%/24h | Drain test на реальном device |
| UI Update Lag | < 16ms | Frame time profiling |
| Accuracy | ±5% | vs `ifconfig` output |

---

## 10. TESTING STRATEGY

### Unit Tests
```kotlin
class NetworkStatsDataSourceTest {
    @Test fun testParsingValid_DevFile()
    @Test fun testCalculateDelta()
    @Test fun testBytesToMbpsConversion()
    @Test fun testPeakTracking()
    @Test fun testPerAppStatsParsing()
}
```

### Integration Tests
```kotlin
class NetworkStatsIntegrationTest {
    @Test fun testRealDeviceNetworkMonitoring()
    @Test fun testNetworkTypeDetection()
    @Test fun testPerAppTrafficAccuracy()
}
```

### Benchmark Tests
```kotlin
@BenchmarkRule
class NetworkStatsBenchmark {
    @Benchmark fun benchmarkDevFileParsing()
    @Benchmark fun benchmarkPerAppStatsParsing()
    @Benchmark fun benchmarkFullCycle()
}
```

---

## 11. DOCUMENTATION & CODE STANDARDS

### Code Style
- Kotlin style guide (Google)
- Clean Architecture patterns
- SOLID principles
- Comprehensive KDoc comments

### Documentation
- Architecture decision records (ADR)
- API documentation
- Example usage
- Troubleshooting guide

---

## 12. ACCEPTANCE CRITERIA

✅ Real-time ingress/egress мониторинг работает  
✅ Accuracy ±5% от реального трафика  
✅ CPU overhead < 1%  
✅ Memory overhead < 20MB  
✅ Per-app трафик показывается (если accessible)  
✅ Network type detection работает  
✅ Overlay отображает метрики корректно  
✅ >80% test coverage  
✅ Нет крашей при различных сценариях  
✅ Performance benchmarks пройдены  

---

## 13. RISKS & MITIGATION

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| `/proc/net/xt_qtaguid` недоступен | High | Medium | Graceful fallback на system-wide stats |
| Per-app stats требуют root | High | Medium | Документировать ограничение, предложить альтернативу |
| Performance overhead выше чем expected | Medium | High | Заранее benchmark'ировать, оптимизировать C++ |
| Compatibility issues на старых Android | Medium | Low | Thorough testing на API 21+ |

---

## 14. SUCCESS METRICS

- ⭐ GitHub stars за фичу (+50 за месяц)
- 📥 Downloads (если на Play Store)
- 💬 Issues (feedback от community)
- ⚡ Performance (CPU/memory в целевых границах)
- 🧪 Test coverage (>80%)

---

## REFERENCES

- Android `/proc/net/dev` documentation
- Linux `/proc` filesystem guide
- ConnectivityManager API reference
- Android Network Security Configuration
- Performance Best Practices

---

**Author:** Senior Android Developer (15+ years)  
**Date:** December 19, 2025  
**Version:** 1.0  
**Status:** Ready for Claude Opus 4.5 Implementation

