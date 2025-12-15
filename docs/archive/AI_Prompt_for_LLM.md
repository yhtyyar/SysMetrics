# ПРОМТ ДЛЯ CLAUDE OPUS 4.5
## SysMetrics Pro - Complete Implementation Guide

**Версия:** 2.0  
**Дата:** 15.12.2025  
**Модель:** Claude Opus 4.5 (Thinking Mode ОБЯЗАТЕЛЬНО!)  

---

## 🚀 НАЧАЛО ПРОМТА

```
Ты — senior Android разработчик с 10+ летним опытом.

ПРОЕКТ: SysMetrics Pro — high-performance Android система мониторинга

ТЕКУЩЕЕ СОСТОЯНИЕ:
✅ MVVM архитектура + Clean Architecture
✅ Floating overlay виджет
✅ CPU/RAM/Температура мониторинг
✅ <50MB оптимизация
✅ Benchmark тесты

ПРОБЛЕМЫ:
❌ Нет Self vs Other apps разделения
❌ Отсутствует 24-hour история (SQLite)
❌ Нет CSV/JSON экспорта
❌ UI не на Material 3
❌ Нет детального анализа памяти
❌ Settings экран неполный
❌ Нет background service

ТВОЯ ЗАДАЧА: Реализовать ВСЕ улучшения по best practices
```

---

## ⚠️ ОЧЕНЬ ВАЖНО: РЕЖИМ THINKING

**Перед тем как писать код:**

1. Используй THINKING MODE (обязательно!)
2. Проанализируй архитектуру SysMetrics
3. Определи все точки интеграции
4. Выяви потенциальные проблемы
5. Спланируй порядок реализации
6. Документируй свой analysis

**Выведи анализ так:**
```
<analysis>
[Твой анализ и планирование]
</analysis>

[ПОТОМ готовый код и тесты]
```

---

## 📋 ФАЗА 1: АНАЛИЗ

### Задачи анализа:

**1. Изучи существующий код:**
- Прочитай всю архитектуру на GitHub
- Поймешь структуру MVVM + Repository
- Определи Hilt DI setup
- Изучи JNI native bridge

**2. Определи точки интеграции:**
- Где добавлять ProcessDataSource?
- Как интегрировать Room Database?
- Куда добавлять новые ViewModel'и?
- Где создавать новые Use Cases?

**3. Выяви проблемы:**
- Какие есть constraints?
- Какие зависимости нужно добавить?
- Где могут быть memory leaks?
- Как оптимизировать performance?

**4. Спланируй реализацию:**
- Какой порядок лучше всего?
- Какие файлы нужно создать/изменить?
- Какие тесты нужно написать?
- Как организовать работу?

---

## 🔧 ФАЗА 2: РЕАЛИЗАЦИЯ

### ЗАДАЧА 1: Process Segmentation (Self vs Other)

#### Требования:
```
✅ Получать список процессов через ActivityManager
✅ Определять Self по PID и package name
✅ Классифицировать Other (system/user)
✅ 95% точность метрик
✅ <50ms выполнение для 300+ процессов
```

#### Структура кода:
```kotlin
// 1. Domain Model
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

// 2. Repository Interface
interface IProcessRepository {
    suspend fun getProcessesWithSegmentation(
        includeSystemApps: Boolean = false
    ): ProcessSegmentation
}

// 3. Use Case
class GetProcessListUseCase @Inject constructor(
    private val processRepository: IProcessRepository
)

// 4. Implementation
class ProcessRepositoryImpl @Inject constructor(
    private val activityManager: ActivityManager,
    private val context: Context
) : IProcessRepository {
    override suspend fun getProcessesWithSegmentation(
        includeSystemApps: Boolean
    ): ProcessSegmentation = withContext(Dispatchers.IO) {
        val selfProcesses = mutableListOf<ProcessInfo>()
        val otherProcesses = mutableListOf<ProcessInfo>()
        
        val runningApps = activityManager.getRunningAppProcesses() ?: emptyList()
        val selfPid = android.os.Process.myPid()
        val selfPackageName = context.packageName
        
        runningApps.forEach { appProcess ->
            val processInfo = parseProcessInfo(appProcess)
            
            if (appProcess.pid == selfPid || 
                appProcess.processName.contains(selfPackageName)) {
                selfProcesses.add(processInfo)
            } else if (includeSystemApps || !isSystemApp(appProcess)) {
                otherProcesses.add(processInfo)
            }
        }
        
        return@withContext ProcessSegmentation(
            selfProcesses = selfProcesses.sortedByDescending { it.memoryBytes },
            otherProcesses = otherProcesses.sortedByDescending { it.memoryBytes },
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun parseProcessInfo(
        appProcess: ActivityManager.RunningAppProcessInfo
    ): ProcessInfo {
        val memInfo = android.os.Debug.MemoryInfo()
        android.os.Debug.getMemoryInfo(appProcess.pid, memInfo)
        
        return ProcessInfo(
            pid = appProcess.pid,
            packageName = appProcess.processName.split(':')[0],
            processName = appProcess.processName,
            memoryBytes = (memInfo.totalPss * 1024L),
            cpuPercent = estimateCpuUsage(appProcess.pid),
            state = mapProcessState(appProcess.importance),
            timestamp = System.currentTimeMillis()
        )
    }
}
```

#### Файлы для создания:
- `data/datasource/ProcessDataSource.kt` (NEW)
- `domain/model/ProcessInfo.kt` (UPDATE с новыми полями)
- `domain/model/ProcessSegmentation.kt` (NEW)
- `domain/repository/IProcessRepository.kt` (UPDATE)
- `data/repository/ProcessRepositoryImpl.kt` (UPDATE)
- `domain/usecase/GetProcessListUseCase.kt` (NEW)

#### Tests:
```kotlin
class ProcessAnalyzerTest {
    @Test
    fun `getProcessesWithSegmentation separates self vs others correctly`() {
        // GIVEN
        val selfPid = 12345
        every { context.packageName } returns "com.example.sysmetrics"
        every { activityManager.getRunningAppProcesses() } returns listOf(
            createMockProcess(pid = selfPid, name = "com.example.sysmetrics"),
            createMockProcess(pid = 5678, name = "com.other.app")
        )
        
        // WHEN
        val result = analyzer.getProcessesWithSegmentation()
        
        // THEN
        assertThat(result.selfProcesses).hasSize(1)
        assertThat(result.otherProcesses).hasSize(1)
        assertThat(result.selfProcesses[0].pid).isEqualTo(selfPid)
    }
}
```

---

### ЗАДАЧА 2: Detailed Memory Analysis

#### Требования:
```
✅ Breakdown: Native, Java Heap, Graphics, Stack, Other
✅ Точность ±5% от Debug.MemoryInfo
✅ Self + top 10 других приложений
✅ Update каждые 2-3 сек
```

#### Структура:
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

class MemoryDataSource @Inject constructor() {
    suspend fun getDetailedMemory(pid: Int): DetailedProcessMemory = 
        withContext(Dispatchers.IO) {
            val memInfo = android.os.Debug.MemoryInfo()
            android.os.Debug.getMemoryInfo(pid, memInfo)
            
            DetailedProcessMemory(
                pid = pid,
                packageName = getPackageName(pid),
                memoryBreakdown = MemoryBreakdown(
                    nativeMemory = memInfo.nativePss * 1024L,
                    javaHeap = memInfo.javaHeapPss * 1024L,
                    graphicsMemory = memInfo.graphicsPss * 1024L,
                    stackMemory = memInfo.stackPss * 1024L,
                    other = memInfo.otherPss * 1024L
                ),
                totalPss = memInfo.totalPss * 1024L,
                timestamp = System.currentTimeMillis()
            )
        }
}
```

#### Файлы:
- `data/datasource/MemoryDataSource.kt` (NEW)
- `domain/model/MemoryBreakdown.kt` (NEW)
- `domain/model/DetailedProcessMemory.kt` (NEW)
- `domain/usecase/GetMemoryDetailedAnalysisUseCase.kt` (NEW)

---

### ЗАДАЧА 3: Room Database для истории (24h)

#### Требования:
```
✅ SQLite через Room
✅ Сохранять каждые 2-3 сек
✅ Auto-cleanup >24h
✅ Max 50MB на диск (~30k записей)
```

#### Код:
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

@Dao
interface MetricsDao {
    @Insert suspend fun insertMetrics(metrics: MetricsEntity)
    @Query("SELECT * FROM metrics_history WHERE timestamp > :since")
    fun getMetricsAfter(since: Long): Flow<List<MetricsEntity>>
    @Query("DELETE FROM metrics_history WHERE createdAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}

@Database(
    entities = [MetricsEntity::class, ProcessEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun metricsDao(): MetricsDao
    
    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "sysmetrics.db"
            ).build()
        }
    }
}
```

#### Cleanup Worker:
```kotlin
class MetricsCleanupWorker(
    context: Context,
    params: WorkerParameters,
    private val database: AppDatabase
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = try {
        val threshold = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        database.metricsDao().deleteOlderThan(threshold)
        Result.success()
    } catch (e: Exception) {
        Timber.e(e, "Cleanup failed")
        Result.retry()
    }
}
```

#### Файлы:
- `data/local/db/AppDatabase.kt` (NEW)
- `data/local/db/entity/MetricsEntity.kt` (NEW)
- `data/local/db/entity/ProcessEntity.kt` (NEW)
- `data/local/db/dao/MetricsDao.kt` (NEW)
- `service/MetricsCleanupWorker.kt` (NEW)
- `data/repository/HistoryRepositoryImpl.kt` (NEW)

---

### ЗАДАЧА 4: CSV/JSON Export

#### Код структуры:
```kotlin
interface IExportRepository {
    suspend fun exportToCsv(days: Int = 1): File
    suspend fun exportToJson(days: Int = 1): File
}

class CsvExporter @Inject constructor(
    private val database: AppDatabase,
    private val context: Context
) {
    suspend fun export(): File = withContext(Dispatchers.IO) {
        val metrics = database.metricsDao()
            .getMetricsAfter(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            .firstOrNull() ?: emptyList()
        
        val csv = buildString {
            appendLine("timestamp,cpu_percent,ram_percent,temperature")
            metrics.forEach { metric ->
                appendLine("${metric.timestamp},${metric.cpuPercent},${metric.ramPercent},${metric.temperature}")
            }
        }
        
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), 
            "metrics_${System.currentTimeMillis()}.csv")
        file.writeText(csv)
        return@withContext file
    }
}

class JsonExporter @Inject constructor(
    private val database: AppDatabase
) {
    suspend fun export(): String = withContext(Dispatchers.IO) {
        val metrics = database.metricsDao()
            .getMetricsAfter(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            .firstOrNull() ?: emptyList()
        
        return@withContext Json.encodeToString(
            mapOf(
                "exportDate" to LocalDateTime.now().toString(),
                "metrics" to metrics
            )
        )
    }
}
```

#### Файлы:
- `utils/exporters/CsvExporter.kt` (NEW)
- `utils/exporters/JsonExporter.kt` (NEW)
- `domain/usecase/ExportMetricsUseCase.kt` (NEW)

---

### ЗАДАЧА 5: Material 3 UI Update

#### Theme обновление:
```kotlin
val LightColors = lightColorScheme(
    primary = Color(0xFF2180A0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB1E5FF),
    secondary = Color(0xFF5E5260),
    tertiary = Color(0xFF7D5260),
    surface = Color(0xFFFCFCF9),
    background = Color(0xFFFCFCF9)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D1E5),
    onPrimary = Color(0xFF134252),
    primaryContainer = Color(0xFF2F5D74),
    secondary = Color(0xFFCCC0CB),
    tertiary = Color(0xFFE7B7C3),
    surface = Color(0xFF1F2121),
    background = Color(0xFF1F2121)
)

@Composable
fun SysMetricsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

#### Файлы:
- `ui/theme/Theme.kt` (UPDATE)
- `ui/theme/Color.kt` (UPDATE)
- `ui/theme/Typography.kt` (UPDATE)

---

### ЗАДАЧА 6: Settings Screen

#### Код:
```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        
        // Monitoring section
        Text("Monitoring", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = uiState.updateInterval.toFloat(),
            onValueChange = { viewModel.setUpdateInterval(it.toInt()) },
            valueRange = 1f..5f,
            steps = 3
        )
        
        Switch(
            checked = uiState.overlayEnabled,
            onCheckedChange = { viewModel.setOverlayEnabled(it) },
            label = "Overlay Enabled"
        )
        
        // Appearance section
        Text("Appearance", style = MaterialTheme.typography.labelLarge)
        SegmentedButtonRow {
            listOf("Light", "Dark", "Auto").forEach { theme ->
                SegmentedButton(
                    selected = uiState.theme == theme,
                    onClick = { viewModel.setTheme(theme) },
                    label = { Text(theme) }
                )
            }
        }
        
        // Data section
        Button(onClick = { viewModel.exportMetrics() }) {
            Text("Export Metrics")
        }
        Button(onClick = { viewModel.clearHistory() }) {
            Text("Clear History")
        }
    }
}

class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val exportRepository: IExportRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState
    
    fun setUpdateInterval(interval: Int) {
        preferencesManager.setUpdateInterval(interval)
    }
    
    fun exportMetrics() = viewModelScope.launch {
        exportRepository.exportToCsv()
    }
}

data class SettingsUiState(
    val updateInterval: Int = 2,
    val overlayEnabled: Boolean = true,
    val backgroundServiceEnabled: Boolean = true,
    val theme: String = "Auto",
    val isExporting: Boolean = false
)
```

#### Файлы:
- `ui/screens/settings/SettingsScreen.kt` (NEW)
- `ui/screens/settings/SettingsViewModel.kt` (NEW)
- `ui/components/SettingsItem.kt` (NEW)

---

### ЗАДАЧА 7: Background Service

#### Код:
```kotlin
@HiltService
class MetricsBackgroundService : Service() {
    
    @Inject
    lateinit var metricsRepository: IMetricsRepository
    
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Main)
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        startMetricsCollection()
        return START_STICKY
    }
    
    private fun startMetricsCollection() {
        coroutineScope.launch {
            metricsRepository.observeMetricsUpdates().collect { metrics ->
                updateNotification(metrics)
            }
        }
    }
    
    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            "metrics",
            "Metrics Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        
        return NotificationCompat.Builder(this, "metrics")
            .setContentTitle("SysMetrics")
            .setSmallIcon(R.drawable.ic_metrics)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
    
    override fun onBind(intent: Intent?) = null
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MetricsBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
```

#### Файлы:
- `service/MetricsBackgroundService.kt` (NEW)
- `service/MetricsWorker.kt` (NEW)

---

## 🧪 ФАЗА 3: ТЕСТИРОВАНИЕ

### Code Quality Standards
```
✅ Kotlin style guide (ktlint)
✅ Static analysis (detekt)
✅ No memory leaks
✅ Proper error handling (Timber)
✅ No deprecated APIs
```

### Unit Tests (70% coverage)
```
ProcessAnalyzerTest
MemoryDataSourceTest
HistoryRepositoryTest
ExporterTest
SettingsViewModelTest
```

### Integration Tests
```
Database operations
File export
Background service
Settings persistence
```

### Performance Tests
```
Metrics updates <16ms
Process parsing <50ms
Memory <150MB
Battery <2%/hour
```

---

## 📚 ФАЗА 4: ДОКУМЕНТАЦИЯ

Документируй:
- [ ] KDoc comments на всех public API
- [ ] Architecture diagrams (ASCII)
- [ ] README update
- [ ] API documentation
- [ ] Testing guide

---

## ⚠️ ОБЯЗАТЕЛЬНЫЕ ПРАВИЛА

### Code Standards
```kotlin
// ✅ ИСПОЛЬЗУЙ:
- suspend functions для async
- Flow для data streams
- StateFlow в ViewModels
- Hilt для DI
- Timber для логирования
- data classes
- sealed classes для state

// ❌ НЕ ИСПОЛЬЗУЙ:
- GlobalScope
- runBlocking на main thread
- Memory leaks
- Hardcoded strings
- Deprecated APIs
- try-catch без логирования
```

### Git Commits
```
feat: Implement process segmentation (self vs other apps)
- Add ProcessDataSource for parsing
- Implement Self/Other classification
- Add unit tests with 95%+ coverage
- Update ViewModel

Closes: #123
```

### Performance Constraints
```
Metrics updates: <16ms (60 FPS)
Process parsing: <50ms
Memory usage: <150MB
Battery drain: <2% per hour
Startup time: <2 sec
Min Android: 8.0 (API 26)
```

---

## 🎯 SUCCESS CRITERIA

✅ Функциональность:
- Process segmentation работает
- Memory breakdown показывается
- 24h история сохранена
- CSV/JSON экспорт работает
- Settings сохраняются
- Background service работает

✅ Качество:
- 70%+ test coverage
- All tests passing
- Performance OK
- No memory leaks
- Code passes ktlint/detekt

✅ Documentation:
- KDoc comments
- Architecture diagrams
- README updated
- API docs
- Testing guide

---

## 🚀 НАЧНИ СЕЙЧАС!

1. **Используй THINKING MODE** (обязательно!)
2. Выведи `<analysis>` раздел с твоим анализом
3. Напиши все файлы по очереди
4. Добавь unit tests
5. Обнови документацию

**НАЧНИ С ФАЗЫ 1 (АНАЛИЗ) - THINKING MODE!**

---

```
[КОНЕЦ ПРОМТА]
```

---

## 💡 СОВЕТЫ ДЛЯ ЭФФЕКТИВНОСТИ

1. **Thinking Mode первым делом** — не пропусти!
2. **Разбей задачи на части** — делай по одной функции
3. **Тесты одновременно с кодом** — не после
4. **Проверяй зависимости** — добавляй в build.gradle.kts
5. **Документируй по ходу** — не в конце

---

## 📞 КОНТАКТЫ

**GitHub:** https://github.com/yhtyyar/SysMetrics  
**Язык:** Kotlin  
**Framework:** Android  
**Min API:** 26  

---

*Промт готов к использованию! Просто копируй и отправь Claude Opus 4.5* 🚀
