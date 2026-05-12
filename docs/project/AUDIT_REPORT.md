# SysMetrics — Комплексный аудит кодовой базы

> **Дата:** 2026-05-12  
> **Ревьюер:** Senior Android Architect (Claude Code)  
> **Версия проекта:** 2.7.0 (versionCode 9, commit 193c791)  
> **Охват:** Архитектура, код-качество, производительность, тестирование, безопасность, DX

---

## Методология

1. Полное сканирование файловой структуры (`app/src/`, `docs/`, `.github/`)
2. Анализ ключевых файлов: `AndroidManifest.xml`, `build.gradle.kts`, `app/src/main/cpp/*.cpp`, `app/src/main/java/**/*.kt`
3. Проверка CI/CD: `android-ci.yml`, `nightly.yml`, `release.yml`
4. Анализ тестового покрытия: unit-тесты, instrumented-тесты, benchmark

---

## 🟢 Сильные стороны

| Категория | Конкретный пример | Почему это хорошо |
|-----------|-------------------|-------------------|
| **Архитектура** | `ISystemMetricsRepository`, `IPreferencesRepository` — интерфейсы в domain-слое | Domain не зависит от Android Framework; легко тестируется unit-тестами |
| **Native Bridge** | `MetricsCollectorFactory` + `FallbackCpuMetricsCollector` | Graceful degradation при отсутствии `.so`; каждый JNI-вызов в `runCatching { }.getOrDefault(-1f)` |
| **Производительность** | `AdaptivePerformanceMonitor` + `BatteryAwareMonitor` | Динамический интервал 500ms–5000ms в зависимости от CPU/RAM/заряда батареи |
| **Native C++** | `native_metrics.cpp`: `fscanf` с форматом `%63[^:]`, `fclose` до return, фиксированные буферы | Корректная оптимизация парсинга `/proc`; флаги `-O3 -ffast-math` обоснованы |
| **TV Support** | `MinimalistOverlayService.kt:377-388` — `FLAG_NOT_TOUCHABLE` для TV | Предотвращение крашей от hover-events на Android TV |
| **DI (Hilt)** | `AppModule.kt` — полный граф: DataSources → Repositories → UseCases | Корректная цепочка зависимостей с `@Singleton` |
| **Flow API** | `SystemMetricsRepository.getMetricsFlow()` с `currentCoroutineContext().isActive` | Правильная отмена coroutine при lifecycle-событиях |
| **Документация** | `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md`, `DEVELOPMENT.md` | Полная структура документации для open-source проекта |
| **CI/CD дизайн** | `concurrency.cancel-in-progress`, AVD-кэш (`actions/cache@v4`), верификация `.so` для 4 ABI | Профессиональный CI с экономией времени и надёжными проверками |
| **LeakCanary** | `debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")` | Memory leak detection в debug-сборках |
| **FileProvider** | `ExportManager.kt:121-128` — `FileProvider.getUriForFile()` + `FLAG_GRANT_READ_URI_PERMISSION` | Корректный безопасный доступ к файлам для sharing |
| **KSP** | Room и Hilt используют KSP вместо KAPT | Быстрее компиляция (до 2x), поддержка Kotlin 2.x |

---

## 🔴 Слабые стороны и риски

### 🔴 CRITICAL

#### 1. `release.keystore` закоммичен в git
- **Локация:** `app/release.keystore`
- **Влияние:** Любой с доступом к репозиторию может подписать вредоносный APK от имени приложения. Нарушение целостности цепочки поставок.
- **Severity:** CRITICAL

#### 2. Двойная подписка на один Flow в `MinimalistOverlayService`
- **Локация:** `service/MinimalistOverlayService.kt`
  - Первый коллектор: `onCreate()`, строки 155–169
  - Второй коллектор: `createOverlayView()` → `loadConfigAndApplySettings()`, строки 271–279
- **Влияние:** Два параллельных коллектора одного `preferencesDataSource.overlayConfig` → race condition при применении настроек, утечка coroutine.
- **Severity:** HIGH

#### 3. Глобальные статики в `native_metrics.cpp` не thread-safe
- **Локация:** `app/src/main/cpp/native_metrics.cpp:13-14`
  ```c
  static CpuStats prev_stats = {0};
  static bool has_prev_stats = false;
  ```
- **Влияние:** Data race при параллельных JNI-вызовах из overlay-сервиса и WorkManager-воркера. Нет `pthread_mutex` или `std::mutex`.
- **Severity:** HIGH

---

### 🟠 HIGH

#### 4. `Timber.plant(DebugTree())` в production-сборках
- **Локация:** `core/SysMetricsApplication.kt:30-32`
- **Влияние:** Нет проверки `BuildConfig.DEBUG`. CPU%, RAM%, сетевые скорости, позиции overlay — всё пишется в `logcat` на production-устройствах.
- **Severity:** HIGH

#### 5. Двойной DI-граф: Hilt + AppContainer
- **Локация:** `service/MinimalistOverlayService.kt:59` — `// @AndroidEntryPoint` закомментировано
- **Детали:** Строки 123–134 тянут зависимости из `appContainer` вручную, при этом создают **новые** экземпляры напрямую:
  ```kotlin
  systemDataSource = SystemDataSource(com.sysmetrics.app.core.di.DefaultDispatcherProvider())
  ```
  Это минует оба DI-графа — в рантайме одновременно существуют два синглтона `SystemDataSource`.
- **Severity:** HIGH

#### 6. CI: Инструментальные тесты полностью отключены
- **Локация:** `.github/workflows/android-ci.yml:135` — `if: false`; `android-tv-tests: if: false` (строка 243)
- **Влияние:** На каждый push/PR выполняются только lint + unit-тесты + build. Регрессии в overlay, сервисе, UI остаются незамеченными.
- **Severity:** HIGH

---

### 🟡 MEDIUM

#### 7. Дублирующиеся классы в двух пакетах
- `utils/DraggableOverlayTouchListener.kt` и `ui/overlay/DraggableOverlayTouchListener.kt`
- `ui/SettingsViewModel.kt` и `ui/settings/SettingsViewModel.kt`
- **Влияние:** Неясно, какой класс используется где; классы могут расходиться в поведении.
- **Severity:** MEDIUM

#### 8. `strtok` в `native_network_stats.cpp` не реентерабелен
- **Локация:** `app/src/main/cpp/native_network_stats.cpp:94`
  ```c
  char* line = strtok(buffer, "\n");
  ```
- **Влияние:** `strtok` использует внутренний статический буфер — не thread-safe при параллельных вызовах. Нужен `strtok_r`.
- **Severity:** MEDIUM

#### 9. Race condition в `MetricsWidgetProvider` companion object
- **Локация:** `widget/MetricsWidgetProvider.kt:26-27`
  ```kotlin
  private var lastCpuTotal = 0L
  private var lastCpuIdle = 0L
  ```
- **Влияние:** `companion object` — синглтон на весь процесс; `onUpdate()` может вызываться параллельно для нескольких виджетов. Некорректные показания CPU.
- **Severity:** MEDIUM

#### 10. `@Volatile` не обеспечивает атомарность составных операций
- **Локация:** `data/repository/SystemMetricsRepository.kt:36`
  ```kotlin
  @Volatile private var previousCpuStats: CpuStats = CpuStats.EMPTY
  ```
- **Влияние:** Read-modify-write в `collectMetrics()` не атомарен между двумя вызывателями. TOCTOU race.
- **Severity:** LOW–MEDIUM

#### 11. `setupExceptionHandler()` заменяет глобальный `UncaughtExceptionHandler`
- **Локация:** `service/MinimalistOverlayService.kt:181-194`
- **Влияние:** Глобальный side effect из сервиса. На TV свайпает `IllegalStateException` с `ACTION_HOVER`, но все остальные необработанные исключения тоже могут потеряться.
- **Severity:** MEDIUM

---

### 🔵 LOW

#### 12. `READ_BUFFER_SIZE=4096` может не вместить `/proc/net/dev`
- **Локация:** `app/src/main/cpp/native_network_stats.cpp:16`
- **Влияние:** На устройствах с 10+ сетевыми интерфейсами (VPN + WiFi + Ethernet + loopback) данные молча обрезаются.

#### 13. `lint { abortOnError = false }` — lint-ошибки не блокируют сборку
- **Локация:** `app/build.gradle.kts:91`
- **Влияние:** Накопление lint-проблем в codebase.

#### 14. Устаревшие зависимости
- Kotlin `1.9.20` (актуальная: `2.x`)
- `kotlinx-coroutines` `1.7.3` (актуальная: `1.9.x`)

#### 15. Большой `docs/archive/` с 55+ файлами
- Build-логи, AI-промты, промежуточные отчёты — создают шум для новых контрибьюторов.

---

## 📊 Оценка по категориям

| Категория | Балл | Комментарий |
|-----------|------|-------------|
| Архитектура | 7/10 | Хорошее Clean Architecture + MVVM, но DI-дуализм (Hilt + AppContainer) — технический долг |
| Код-качество | 6/10 | Двойной Flow-коллектор — баг; дублирующиеся классы; `// @AndroidEntryPoint`; `abortOnError = false` |
| Производительность | 8/10 | Нативный бэкенд реализован грамотно; адаптивный мониторинг — умно; заявленные метрики реалистичны |
| Тестирование | 5/10 | Хорошие unit-тесты (MockK, Turbine), но CI не запускает ни одного instrumented-теста; весь Kaspresso отключён |
| Безопасность | 4/10 | `release.keystore` в git — критично; Timber в release; остальное (no network, FileProvider) — корректно |
| Документация | 8/10 | Отличная структура: README, CONTRIBUTING, SECURITY, CHANGELOG |
| DX/Onboarding | 7/10 | Ясный README; 55+ файлов в `docs/archive/` создают шум |
| **Итого** | **45/70** | Крепкая основа с конкретными, решаемыми проблемами |

---

## 🚀 Рекомендации (приоритизированные)

### P0 — Немедленно

#### 1. Удалить `release.keystore` из git
```bash
git rm --cached app/release.keystore
echo "app/release.keystore" >> .gitignore
# Сгенерировать новый keystore:
keytool -genkey -v -keystore app/release.keystore -alias key0 -keyalg RSA -keysize 2048 -validity 10000
```
**Ожидаемый эффект:** Устранение критической уязвимости цепочки поставок.

#### 2. Исправить двойной Flow-коллектор в `MinimalistOverlayService`
Удалить метод `loadConfigAndApplySettings()` (строки 271–279) и его вызов в `createOverlayView()` (строка 337).
Функциональность уже покрыта коллектором в `onCreate()` (строки 155–169).

**Ожидаемый эффект:** Устранение race condition и утечки coroutine.

#### 3. Guard `Timber.plant` с `BuildConfig.DEBUG`
```kotlin
// SysMetricsApplication.kt
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```
**Ожидаемый эффект:** Производственные данные CPU/RAM не попадают в logcat.

---

### P1 — В течение спринта

#### 4. Добавить mutex в `native_metrics.cpp`
```c
#include <pthread.h>
static pthread_mutex_t cpu_stats_mutex = PTHREAD_MUTEX_INITIALIZER;

// В getCpuUsage():
pthread_mutex_lock(&cpu_stats_mutex);
// ... вся логика prev_stats/has_prev_stats ...
pthread_mutex_unlock(&cpu_stats_mutex);
```

#### 5. Заменить `strtok` на `strtok_r` в `native_network_stats.cpp`
```c
char* saveptr = NULL;
char* line = strtok_r(buffer, "\n", &saveptr);
while (line && count < max_count) {
    // ...
    line = strtok_r(NULL, "\n", &saveptr);
}
```

#### 6. Исправить race condition в `MetricsWidgetProvider`
```kotlin
companion object {
    @Volatile private var lastCpuTotal = 0L
    @Volatile private var lastCpuIdle = 0L
    // или использовать synchronized(this) { ... }
}
```

#### 7. Завершить Hilt-миграцию в `MinimalistOverlayService`
```kotlin
@AndroidEntryPoint
class MinimalistOverlayService : LifecycleService() {
    @Inject lateinit var metricsCollector: IMetricsCollector
    @Inject lateinit var processStatsCollector: IProcessStatsCollector
    @Inject lateinit var stringFormatter: IStringFormatter
    @Inject lateinit var deviceUtils: DeviceUtils
    // Убрать: val appContainer = (application as SysMetricsApplication).appContainer
}
```

#### 8. Включить instrumented-тесты в CI
Использовать API 28 с `target: default` (стабильнее на GitHub runners):
```yaml
instrumented-tests:
  if: true   # убрать if: false
  matrix:
    include:
      - api-level: 28
        target: default
        arch: x86
```

---

### P2 — Технический долг

#### 9. Удалить дублирующиеся классы
- Оставить `ui/overlay/DraggableOverlayTouchListener.kt`, удалить `utils/DraggableOverlayTouchListener.kt`
- Оставить `ui/settings/SettingsViewModel.kt`, удалить `ui/SettingsViewModel.kt`

#### 10. Убрать закомментированный `enableDragging()` (дублируется дважды в строках 412 и 415)

#### 11. Включить `lint.abortOnError = true`

#### 12. Архивировать `docs/archive/` или удалить build-логи и промежуточные отчёты

---

## 🔮 Стратегические направления

### 1. Завершение Hilt-миграции
**Обоснование:** AppContainer — «переходный» артефакт, но живёт уже 47+ коммитов.  
**Первые шаги:** MinimalistOverlayService (`@AndroidEntryPoint`) → удалить AppContainer → перенести MetricsCollectorFactory в `@Provides`.  
**Риски:** OverlayService требует тестирования после изменения.

### 2. C++ Unit Testing с GoogleTest
**Обоснование:** `native_metrics.cpp` парсит критические данные без единого теста.  
**Первые шаги:** Добавить `googletest` через CMakeLists.txt; покрыть `calculate_cpu_usage()`, `parse_interface_line()`, `format_time_string()`.

### 3. Восстановление Kaspresso-тестов
**Обоснование:** `feature/kaspresso-testing` содержит готовую инфраструктуру.  
**Первые шаги:** Merge только `MinimalTest.kt` в main; постепенно включать `SettingsTest`, `SmokeTest`.

### 4. Code Coverage
**Первые шаги:** JaCoCo plugin в `app/build.gradle.kts`; загрузка отчёта в CI; минимальный порог 70%.

---

## 🎯 Предлагаемые PR (первоочередные)

| # | PR | Сложность | Ценность |
|---|-----|-----------|---------|
| 1 | `security: remove keystore, rotate signing` | Низкая | Критичная |
| 2 | `fix(overlay): remove duplicate flow collector` | Низкая | Высокая |
| 3 | `fix(logging): guard Timber.plant with BuildConfig.DEBUG` | Тривиальная | Высокая |
| 4 | `fix(native): add mutex to cpu stats + strtok_r` | Средняя | Высокая |
| 5 | `feat(di): complete Hilt migration in MinimalistOverlayService` | Средняя | Средняя |

---

## Шаблон PR (`.github/pull_request_template.md`)

```markdown
## Что изменено
<!-- 1-3 bullet points -->

## Тип изменения
- [ ] fix — исправление бага
- [ ] feat — новая функциональность
- [ ] refactor — без изменения поведения
- [ ] perf — оптимизация производительности
- [ ] test — добавление/правка тестов
- [ ] ci — изменения pipeline

## Чеклист
- [ ] Прошли unit-тесты: `./gradlew :app:testDebugUnitTest`
- [ ] Прошёл lint: `./gradlew :app:lintDebug`
- [ ] Для native-изменений: проверен fallback-путь (Kotlin)
- [ ] Для overlay-изменений: протестировано на TV и Mobile
- [ ] Обновлён CHANGELOG.md (если публичное изменение)

## Скриншоты / видео (для UI-изменений)

## Связанные issues
Closes #
```

---

*Документ создан: 2026-05-12. Следующий аудит рекомендован после завершения Hilt-миграции.*
