# SysMetrics Pro - Development Guide

**Version:** 2.0  
**Date:** December 15, 2025  
**Language:** Kotlin 1.9  

---

## 📋 Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [Dependencies](#dependencies)
4. [Code Standards](#code-standards)
5. [Testing Guide](#testing-guide)
6. [Performance Optimization](#performance-optimization)
7. [Debugging](#debugging)
8. [Build & Deploy](#build--deploy)

---

## 🏗️ Architecture Overview

### Architectural Pattern

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

- ✅ **Single Responsibility** - Each class has one reason to change
- ✅ **Dependency Inversion** - DI via Hilt
- ✅ **Separation of Concerns** - Clear architectural layers
- ✅ **Observable State** - Flow/StateFlow for reactive updates
- ✅ **Testability** - Mockable dependencies via interfaces

---

## 📂 Project Structure

```
app/src/main/
├── cpp/                          # Native C++ code
│   ├── CMakeLists.txt
│   ├── native_metrics.h
│   └── native_metrics.cpp
├── java/com/sysmetrics/app/
│   ├── core/
│   │   ├── common/              # Constants, Result wrapper
│   │   ├── di/                  # DispatcherProvider
│   │   └── extensions/          # Kotlin extensions
│   ├── data/
│   │   ├── model/               # Data models
│   │   ├── repository/          # Repository implementations
│   │   └── source/              # Data sources
│   ├── domain/
│   │   ├── collector/           # Collector interfaces
│   │   ├── repository/          # Repository interfaces
│   │   └── usecase/             # Business logic
│   ├── native_bridge/
│   │   └── NativeMetrics.kt     # JNI wrapper
│   ├── ui/
│   │   ├── overlay/
│   │   ├── MainActivity.kt
│   │   └── *ViewModel.kt
│   ├── service/
│   │   └── OverlayService.kt
│   ├── utils/
│   │   ├── MetricsCollector.kt
│   │   ├── ProcessStatsCollector.kt
│   │   └── AdaptivePerformanceMonitor.kt
│   └── di/
│       ├── AppModule.kt
│       └── CollectorModule.kt
└── androidTest/
    └── benchmark/               # Performance benchmarks
```

---

## 📦 Dependencies

### Core Dependencies

```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-service:2.6.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    
    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Database
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // UI - Material 3
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material3:material3:1.1.2")
    
    // Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Background Work
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Memory Leak Detection (Debug)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.room:room-testing:2.6.0")
}

plugins {
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.google.dagger.hilt.android")
}
```

---

## 📝 Code Standards

### Naming Conventions

```kotlin
// Classes & Interfaces
class MetricsCollector { }
interface IProcessRepository { }
sealed class UiState { }
data class SystemMetrics { }

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

### Function Documentation Template

```kotlin
/**
 * Collects process segmentation data separating self vs other apps
 *
 * @param includeSystemApps whether to include system applications
 * @return [ProcessSegmentation] with separated process lists
 * @throws IOException if /proc reading fails
 *
 * Example:
 * ```
 * val segmentation = repository.getProcesses(includeSystemApps = false)
 * println(segmentation.selfProcesses.size)
 * ```
 */
suspend fun getProcessesWithSegmentation(
    includeSystemApps: Boolean = false
): ProcessSegmentation = withContext(dispatcherProvider.io) {
    try {
        // Implementation
        collectProcessData(includeSystemApps)
    } catch (e: Exception) {
        Timber.e(e, "Failed to collect processes")
        throw ProcessCollectionException("Could not fetch processes", e)
    }
}
```

### ViewModel Template

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val metricsUseCase: GetSystemMetricsUseCase,
    private val processUseCase: GetProcessListUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DashboardUiState>(
        DashboardUiState.Loading
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        observeMetrics()
    }
    
    private fun observeMetrics() {
        viewModelScope.launch {
            try {
                metricsUseCase()
                    .collect { metrics ->
                        _uiState.value = DashboardUiState.Success(metrics)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load metrics")
                _uiState.value = DashboardUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }
    
    fun refresh() {
        _uiState.value = DashboardUiState.Loading
        observeMetrics()
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val metrics: SystemMetrics) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
```

### Error Handling Best Practices

```kotlin
// ✅ CORRECT - Graceful degradation
suspend fun getData(): Flow<Data> = flow {
    try {
        val data = fetchFromSource()
        emit(data)
    } catch (e: IOException) {
        Timber.e(e, "Network fetch failed, using cache")
        emit(getCachedData())
    } catch (e: Exception) {
        Timber.e(e, "Unexpected error")
        throw DataFetchException("Failed to load data", e)
    }
}

// ❌ INCORRECT - Silent failure
suspend fun getData(): Data {
    val data = fetchFromSource() // may throw
    return data
}
```

### Logging with Timber

```kotlin
// Initialize in Application class
class SysMetricsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

// Usage
Timber.tag("METRICS_CPU").d("CPU usage: %.1f%%", cpuPercent)
Timber.tag("OVERLAY_UPDATE").i("Update cycle completed in %dms", duration)
Timber.tag("DB_WRITE").w("Database write took longer than expected")
Timber.tag("PROC_ERROR").e(exception, "Failed to parse process data")
```

---

## 🧪 Testing Guide

### Unit Test Template

```kotlin
@RunWith(JUnit4::class)
class ProcessRepositoryTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var repository: ProcessRepositoryImpl
    private val mockActivityManager = mockk<ActivityManager>()
    private val mockContext = mockk<Context>()
    
    @Before
    fun setup() {
        repository = ProcessRepositoryImpl(
            activityManager = mockActivityManager,
            context = mockContext
        )
    }
    
    @Test
    fun `getProcesses returns segmented list`() = runTest {
        // GIVEN
        val selfPid = Process.myPid()
        every { mockContext.packageName } returns "com.sysmetrics.app"
        every { mockActivityManager.runningAppProcesses } returns listOf(
            createMockProcess(pid = selfPid, name = "com.sysmetrics.app"),
            createMockProcess(pid = 5678, name = "com.other.app")
        )
        
        // WHEN
        val result = repository.getProcessesWithSegmentation()
        
        // THEN
        assertThat(result.selfProcesses).hasSize(1)
        assertThat(result.otherProcesses).hasSize(1)
        assertThat(result.selfProcesses[0].pid).isEqualTo(selfPid)
    }
    
    @Test(expected = ProcessCollectionException::class)
    fun `getProcesses throws on error`() = runTest {
        every { mockActivityManager.runningAppProcesses } throws IOException()
        repository.getProcessesWithSegmentation()
    }
}
```

### Database Test

```kotlin
@RunWith(AndroidJUnit4::class)
class MetricsDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: MetricsDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
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
        val result = dao.getMetricsAfter(0).first()
        
        // THEN
        assertThat(result).isNotEmpty()
        assertThat(result[0].cpuPercent).isEqualTo(42.5f)
    }
    
    @Test
    fun autoCleanupOldData() = runTest {
        // GIVEN
        val oldTimestamp = System.currentTimeMillis() - 25 * 60 * 60 * 1000
        dao.insertMetrics(MetricsEntity(
            timestamp = oldTimestamp,
            cpuPercent = 10f,
            ramPercent = 20f
        ))
        
        // WHEN
        val threshold = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        dao.deleteOlderThan(threshold)
        
        // THEN
        val result = dao.getMetricsAfter(0).first()
        assertThat(result).isEmpty()
    }
}
```

### ViewModel Test

```kotlin
@RunWith(JUnit4::class)
class DashboardViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: DashboardViewModel
    private val mockUseCase = mockk<GetSystemMetricsUseCase>()
    
    @Before
    fun setup() {
        viewModel = DashboardViewModel(mockUseCase, SavedStateHandle())
    }
    
    @Test
    fun `uiState updates when metrics received`() = runTest {
        // GIVEN
        val metrics = SystemMetrics(
            cpu = CpuMetrics(usage = 42.5f, timestamp = 0L),
            ram = RamMetrics(percent = 65.0f, timestamp = 0L)
        )
        
        every { mockUseCase() } returns flowOf(metrics)
        
        // WHEN
        viewModel.refresh()
        
        // THEN
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(DashboardUiState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(DashboardUiState.Success::class.java)
        }
    }
}
```

---

## ⚡ Performance Optimization

### 1. Memory Management

```kotlin
// ❌ BAD - Creates unnecessary copies
val list = getAllMetrics().toList()

// ✅ GOOD - Stream with Flow
fun getAllMetrics(): Flow<Metrics> = flow {
    database.metricsDao().getAll().collect { emit(it) }
}
```

### 2. Coroutine Scope Management

```kotlin
// ❌ BAD - Memory leak risk
GlobalScope.launch {
    // Long operation
}

// ✅ GOOD - Lifecycle-aware
viewModelScope.launch {
    // Cancelled when ViewModel destroyed
}

// ✅ GOOD - Service lifecycle
lifecycleScope.launch {
    // Cancelled with lifecycle
}
```

### 3. Database Optimization

```kotlin
// ❌ BAD - N+1 query problem
val processes = getAllProcesses()
processes.forEach { process ->
    val memory = getMemory(process.pid) // Query per item
}

// ✅ GOOD - Single query with join
@Query("""
    SELECT p.*, m.* FROM processes p
    LEFT JOIN memory m ON p.pid = m.pid
    WHERE p.timestamp > :since
""")
fun getProcessesWithMemory(since: Long): Flow<List<ProcessWithMemory>>
```

### 4. Flow vs StateFlow

```kotlin
// ❌ BAD - Creates new flow each time
fun getMetrics(): Flow<Metrics> = flow {
    emit(calculateMetrics())
}

// ✅ GOOD - Cached shared flow
private val _metrics = MutableStateFlow(Metrics())
val metrics: StateFlow<Metrics> = _metrics.asStateFlow()
```

---

## 🔍 Debugging

### Logcat Filtering

```bash
# Show only SysMetrics logs
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_"

# Show with timestamps
adb logcat -v time | grep METRICS_CPU

# Clear logcat
adb logcat -c

# Save to file
adb logcat -d > logcat.txt
```

### Android Profiler

1. **Run → Profile** in Android Studio
2. **Memory Tab** - Monitor heap size and allocations
3. **CPU Tab** - Analyze method traces
4. **Energy Tab** - Check battery impact

### Database Inspection

```kotlin
// Debug build database logging
@Provides
@Singleton
fun provideDatabase(context: Context): AppDatabase {
    val builder = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "sysmetrics.db"
    )
    
    if (BuildConfig.DEBUG) {
        builder.setQueryCallback { sqlQuery, bindArgs ->
            Timber.tag("DB_QUERY").d("SQL: $sqlQuery\nArgs: $bindArgs")
        }
    }
    
    return builder.build()
}
```

### Performance Measurement

```kotlin
private inline fun <T> measureTime(label: String, block: () -> T): T {
    val start = System.nanoTime()
    val result = block()
    val durationMs = (System.nanoTime() - start) / 1_000_000
    Timber.tag("PERF").d("$label took ${durationMs}ms")
    return result
}

// Usage
val metrics = measureTime("Process collection") {
    processRepository.getProcesses()
}
```

---

## 🔨 Build & Deploy

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
./gradlew testDebugUnitTest jacocoTestReport

# Benchmark tests
./gradlew :app:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.sysmetrics.app.benchmark.MetricsParserBenchmark
```

### Code Quality

```bash
# Kotlin lint
./gradlew ktlintCheck

# Static analysis
./gradlew detekt

# All checks
./gradlew check
```

---

## 🔒 Best Practices Checklist

- ✅ **Use Hilt for DI** - Never create dependencies manually
- ✅ **Scope coroutines properly** - viewModelScope, lifecycleScope
- ✅ **Handle exceptions gracefully** - Always log and provide fallbacks
- ✅ **Use Flow for streams** - Prefer over LiveData
- ✅ **Write tests alongside code** - Not after
- ✅ **Use sealed classes for state** - Type-safe state management
- ✅ **Document public APIs** - KDoc comments
- ✅ **Keep functions small** - Single responsibility
- ✅ **Avoid memory leaks** - Use LeakCanary
- ✅ **Monitor performance** - Regular profiling

---

## 📞 Quick Reference

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
    val result = withContext(dispatcherProvider.io) {
        useCase.execute()
    }
}

// Observe in Activity/Fragment
lifecycleScope.launch {
    viewModel.state.collect { state ->
        updateUI(state)
    }
}

// Room operations
@Insert suspend fun insert(entity: MyEntity)
@Query("SELECT * FROM table WHERE id = :id")
fun getById(id: Int): Flow<MyEntity>

// Timber logging
Timber.tag("TAG").d("Message: %s", value)
Timber.tag("ERROR").e(exception, "Error occurred")
```

---

*Last updated: December 15, 2025*
