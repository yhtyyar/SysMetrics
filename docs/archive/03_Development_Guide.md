# ДОКУМЕНТАЦИЯ ДЛЯ РАЗРАБОТКИ
## SysMetrics Pro - Development Guide

**Версия:** 2.0  
**Дата:** 15.12.2025  
**Язык:** Kotlin  

---

## 📋 СОДЕРЖАНИЕ

1. [Обзор архитектуры](#обзор-архитектуры)
2. [Structure проекта](#structure-проекта)
3. [Dependencies](#dependencies)
4. [Code Standards](#code-standards)
5. [Testing Guide](#testing-guide)
6. [Performance Tips](#performance-tips)
7. [Debugging](#debugging)

---

## 🏗️ ОБЗОР АРХИТЕКТУРЫ

### Архитектурный паттерн
```
┌─────────────────────────────────────────┐
│   UI Layer (MVVM + Compose/XML)         │
│   Screens, ViewModels, Composables      │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────┴───────────────────────┐
│   Domain Layer                          │
│   UseCases, Repository Interfaces       │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────┴───────────────────────┐
│   Data Layer                            │
│   Repository Impl, DataSources          │
└─────────────────┬───────────────────────┘
                  │
    ┌─────────────┼─────────────┐
    ↓             ↓             ↓
┌──────────┐ ┌──────────┐ ┌──────────┐
│  Room    │ │ System   │ │ Prefs    │
│ Database │ │  APIs    │ │ Manager  │
└──────────┘ └──────────┘ └──────────┘
```

### Design Principles
- ✅ Single Responsibility Principle (SRP)
- ✅ Dependency Inversion (DI через Hilt)
- ✅ Separation of Concerns (слои архитектуры)
- ✅ Observable state (Flow/StateFlow)
- ✅ Testability (мокируемые зависимости)

---

## 📂 STRUCTURE ПРОЕКТА

### Directory Layout
```
app/src/main/kotlin/com/example/sysmetrics/
├── data/
│   ├── datasource/
│   │   ├── ProcessDataSource.kt
│   │   ├── MemoryDataSource.kt
│   │   └── SystemMetricsDataSource.kt
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── entity/
│   │   │   │   ├── MetricsEntity.kt
│   │   │   │   └── ProcessEntity.kt
│   │   │   └── dao/
│   │   │       └── MetricsDao.kt
│   │   └── preferences/
│   │       └── PreferencesManager.kt
│   └── repository/
│       ├── ProcessRepositoryImpl.kt
│       ├── HistoryRepositoryImpl.kt
│       └── ExportRepositoryImpl.kt
├── domain/
│   ├── repository/
│   │   ├── IProcessRepository.kt
│   │   ├── IHistoryRepository.kt
│   │   └── IExportRepository.kt
│   ├── usecase/
│   │   ├── GetProcessListUseCase.kt
│   │   ├── GetMemoryAnalysisUseCase.kt
│   │   └── ExportMetricsUseCase.kt
│   └── model/
│       ├── ProcessInfo.kt
│       ├── ProcessSegmentation.kt
│       ├── MemoryBreakdown.kt
│       └── SystemMetrics.kt
├── ui/
│   ├── screens/
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt
│   │   │   └── DashboardViewModel.kt
│   │   ├── details/
│   │   │   ├── DetailsScreen.kt
│   │   │   └── DetailsViewModel.kt
│   │   └── settings/
│   │       ├── SettingsScreen.kt
│   │       └── SettingsViewModel.kt
│   ├── components/
│   │   ├── MetricsCard.kt
│   │   ├── CpuChart.kt
│   │   ├── RamChart.kt
│   │   └── ProcessListItem.kt
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Typography.kt
├── service/
│   ├── MetricsBackgroundService.kt
│   ├── MetricsCleanupWorker.kt
│   └── MetricsWorker.kt
├── utils/
│   ├── exporters/
│   │   ├── CsvExporter.kt
│   │   └── JsonExporter.kt
│   ├── formatters/
│   │   └── DataFormatter.kt
│   └── extensions/
│       └── Ext.kt
└── di/
    └── AppModule.kt
```

---

## 📦 DEPENDENCIES

### Добавить в build.gradle.kts

```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Lifecycle & Architecture
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    
    // DI
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Database
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // UI - Compose
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.0")
    
    // UI - XML (если используется)
    implementation("com.google.android.material:material:1.10.0")
    
    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // OR
    // implementation("com.patrykandpatrick.vico:core:1.9.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Permissions
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")
    
    // Background work
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.room:room-testing:2.6.0")
}

plugins {
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}
```

---

## 📝 CODE STANDARDS

### Kotlin Naming Conventions
```kotlin
// Classes & Interfaces
class ProcessAnalyzer { }
interface IProcessRepository { }
sealed class ProcessState { }
data class ProcessInfo { }

// Variables & Functions
val processList: List<ProcessInfo> = emptyList()
var currentMetrics: SystemMetrics? = null
suspend fun getMetrics(): CpuMetrics { }
fun calculateUsage(value: Float): Float { }

// Constants
companion object {
    private const val DEFAULT_INTERVAL = 2000L
    private const val MAX_CACHE_SIZE = 100
}

// Private/Public
private fun internalCalculation() { }
fun publicFunction() { }
protected var protectedVar: String = ""
```

### Function Template
```kotlin
/**
 * Получает список процессов разделённых на Self vs Other
 *
 * @param includeSystemApps включать ли системные приложения
 * @return ProcessSegmentation с разделёнными процессами
 * @throws IOException если ошибка чтения /proc
 *
 * Example:
 * ```
 * val segmentation = analyzer.getProcesses(includeSystemApps = false)
 * println(segmentation.selfProcesses.size)
 * ```
 */
suspend fun getProcessesWithSegmentation(
    includeSystemApps: Boolean = false
): ProcessSegmentation = withContext(Dispatchers.IO) {
    try {
        // Implementation
    } catch (e: Exception) {
        Timber.e(e, "Failed to get processes")
        throw ProcessingException("Could not fetch processes", e)
    }
}
```

### ViewModel Template
```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val metricsUseCase: GetMetricsUseCase,
    private val processUseCase: GetProcessListUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DashboardUiState>(
        DashboardUiState.Loading
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadMetrics()
    }
    
    private fun loadMetrics() {
        viewModelScope.launch {
            try {
                metricsUseCase()
                    .collect { metrics ->
                        _uiState.value = DashboardUiState.Success(metrics)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load metrics")
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val metrics: SystemMetrics) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
```

### Error Handling
```kotlin
// ✅ ПРАВИЛЬНО
suspend fun getData(): Flow<Data> = flow {
    try {
        val data = fetchFromSource()
        emit(data)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch data")
        // Graceful degradation
        emit(getCachedData())
    }
}

// ❌ НЕПРАВИЛЬНО
suspend fun getData(): Data {
    val data = fetchFromSource() // может выбросить исключение
    return data
}
```

### Logging with Timber
```kotlin
// Инициализация в Application
class SysMetricsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

// Usage
Timber.d("Debug message")
Timber.i("Info message")
Timber.w("Warning message")
Timber.e(exception, "Error with exception")
Timber.wtf("What a terrible failure")
```

---

## 🧪 TESTING GUIDE

### Unit Test Template
```kotlin
@RunWith(RobolectricTestRunner::class)
class ProcessAnalyzerTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var analyzer: ProcessAnalyzer
    private val mockContext = mockk<Context>()
    private val mockActivityManager = mockk<ActivityManager>()
    
    @Before
    fun setup() {
        analyzer = ProcessAnalyzer(mockContext, mockActivityManager)
    }
    
    @Test
    fun `getProcesses returns non-empty list`() = runTest {
        // GIVEN
        every { mockActivityManager.getRunningAppProcesses() } returns listOf(
            mockk<ActivityManager.RunningAppProcessInfo>()
        )
        
        // WHEN
        val result = analyzer.getProcessesWithSegmentation()
        
        // THEN
        assertThat(result.selfProcesses).isNotEmpty()
    }
    
    @Test
    fun `getProcesses correctly separates self vs others`() = runTest {
        // GIVEN
        val selfPid = 12345
        every { mockContext.packageName } returns "com.example.sysmetrics"
        every { mockActivityManager.getRunningAppProcesses() } returns listOf(
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
    
    @Test(expected = IOException::class)
    fun `getProcesses throws exception on error`() = runTest {
        every { mockActivityManager.getRunningAppProcesses() } throws IOException()
        analyzer.getProcessesWithSegmentation()
    }
}
```

### Database Test
```kotlin
@RunWith(AndroidRunner::class)
class MetricsDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: MetricsDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.metricsDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertAndQuery() = runTest {
        // GIVEN
        val metrics = MetricsEntity(
            timestamp = System.currentTimeMillis(),
            cpuPercent = 42.5f,
            ramPercent = 65.0f
        )
        
        // WHEN
        dao.insertMetrics(metrics)
        val result = dao.getMetricsAfter(0).firstOrNull()
        
        // THEN
        assertThat(result).isNotNull()
        assertThat(result?.cpuPercent).isEqualTo(42.5f)
    }
}
```

### ViewModel Test
```kotlin
class DashboardViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: DashboardViewModel
    private val mockUseCase = mockk<GetMetricsUseCase>()
    
    @Before
    fun setup() {
        viewModel = DashboardViewModel(mockUseCase, SavedStateHandle())
    }
    
    @Test
    fun `uiState updates when metrics received`() = runTest {
        // GIVEN
        val metrics = SystemMetrics(
            cpu = CpuMetrics(42.5f, emptyList(), 1.8f, System.currentTimeMillis()),
            ram = RamMetrics(6291456, 3145728, 2097152, 1048576, 50f, System.currentTimeMillis()),
            processes = emptyList(),
            timestamp = System.currentTimeMillis()
        )
        
        every { mockUseCase() } returns flowOf(metrics)
        
        // WHEN
        viewModel.loadMetrics()
        
        // THEN
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(DashboardUiState.Success::class.java)
    }
}
```

---

## ⚡ PERFORMANCE TIPS

### 1. Memory Optimization
```kotlin
// ❌ ПЛОХО - Creates copy
val list = getAllMetrics().toList()

// ✅ ХОРОШО - Use Flow for streaming
fun getAllMetrics(): Flow<Metrics> = flow {
    database.metricsDao().getAll().collect {
        emit(it)
    }
}
```

### 2. Coroutine Scope Management
```kotlin
// ❌ ПЛОХО - Memory leak
GlobalScope.launch {
    // долгая операция
}

// ✅ ХОРОШО - Scoped
viewModelScope.launch {
    // операция будет отменена при уничтожении ViewModel
}
```

### 3. Database Queries
```kotlin
// ❌ ПЛОХО - N+1 проблема
val processes = getAllProcesses()
processes.forEach { process ->
    val memory = getMemory(process.pid) // query на каждое
}

// ✅ ХОРОШО - Single query
@Query("""
    SELECT * FROM process_history
    WHERE timestamp > :since
""")
fun getProcessesWithMetrics(since: Long): Flow<List<ProcessInfo>>
```

### 4. Flow/StateFlow Usage
```kotlin
// ❌ ПЛОХО - Creates new flow each time
fun getMetrics(): Flow<Metrics> = flow {
    emit(calculateMetrics())
}

// ✅ ХОРОШО - Cached flow
private val _metrics = MutableStateFlow(Metrics())
val metrics: StateFlow<Metrics> = _metrics.asStateFlow()
```

---

## 🔍 DEBUGGING

### Logcat Filtering
```bash
# Show only SysMetrics logs
adb logcat | grep sysmetrics

# Show with timestamps
adb logcat -v time | grep sysmetrics

# Clear logcat
adb logcat -c
```

### Android Profiler
```
1. Run → Profile
2. Tab "Memory" - смотрить heap size
3. Tab "CPU" - смотрить процессор
4. Tab "Network" - если нужно (для будущего)
5. Tab "Energy" - батарея
```

### Database Debugging
```kotlin
// В debug build'е добавить
Room.databaseBuilder(context, AppDatabase::class.java, "db")
    .setQueryCallback { sqlQuery, bindArgs ->
        Timber.d("SQL: $sqlQuery, Args: $bindArgs")
    }
    .build()
```

### Performance Monitoring
```kotlin
private inline fun <T> measureTime(label: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    val duration = System.currentTimeMillis() - start
    Timber.d("$label took ${duration}ms")
    return result
}

// Usage
measureTime("Process parsing") {
    analyzer.getProcesses()
}
```

---

## 📊 BUILD & RUN

### Debug Build
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
./gradlew assembleRelease
```

### Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# With coverage
./gradlew testDebugUnitTest --coverage
```

---

## 🔒 BEST PRACTICES

1. ✅ **Always use Hilt for DI** - не создавай объекты вручную
2. ✅ **Scope coroutines properly** - viewModelScope, lifecycleScope
3. ✅ **Handle exceptions gracefully** - не игнорируй ошибки
4. ✅ **Use Flow for data streams** - не LiveData для новых проектов
5. ✅ **Write tests alongside code** - не после
6. ✅ **Use sealed classes for state** - type-safe state management
7. ✅ **Document public APIs** - KDoc comments
8. ✅ **Keep functions small** - single responsibility
9. ✅ **Avoid memory leaks** - отписывайся от Flow'ов
10. ✅ **Monitor performance** - используй Profiler

---

## 📞 QUICK REFERENCE

```kotlin
// Hilt Injection
@HiltViewModel
class MyViewModel @Inject constructor(
    private val useCase: MyUseCase
) : ViewModel()

// Flow in ViewModel
private val _state = MutableStateFlow(State())
val state: StateFlow<State> = _state.asStateFlow()

// Launch coroutine
viewModelScope.launch {
    val result = useCase.execute()
}

// Observe in Compose
val state by viewModel.state.collectAsState()

// Room Insert
@Insert
suspend fun insert(entity: MyEntity)

// Room Query
@Query("SELECT * FROM table WHERE id = :id")
fun getById(id: Int): Flow<MyEntity>

// Timber Log
Timber.d("Debug: %s", value)
Timber.e(exception, "Error occurred")
```

---

*Документация готова к использованию!* ✅
