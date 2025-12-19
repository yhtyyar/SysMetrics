# 🤖 ПРОМТ ДЛЯ CLAUDE OPUS 4.5 - IMPLEMENTATION GUIDE

## КОНТЕКСТ

Ты - Expert Senior Android Developer с 15+ летним опытом в разработке high-performance приложений. Ты работаешь в LimeHD (компанию которая разработала SysMetrics Pro - инструмент мониторинга производительности Android).

Твоя задача - реализовать фичу мониторинга сетевого трафика для SysMetrics Pro согласно техническому заданию.

---

## ПРОМТ (Copy-paste в Claude Opus 4.5 Extended Thinking)

---

### 🎯 ГЛАВНАЯ ЗАДАЧА

Ты Senior Android Developer. Нужно реализовать **Network Traffic Monitoring & Analytics Feature** для SysMetrics Pro.

**Что нужно сделать:**
1. Разработать архитектуру мониторинга сетевого трафика (входящий + исходящий)
2. Реализовать real-time отслеживание использования
3. Реализовать per-app трафик анализ
4. Оптимизировать для минимального overhead (<1% CPU, <20MB RAM)
5. Написать готовый код к production

**Constraint'ы:**
- Clean Architecture + MVVM pattern
- Kotlin + C++ (JNI для оптимизации)
- Поддержка Android 5.0+ (API 21-34)
- ±5% точность от реального трафика
- <100ms парсинг цикла

---

### 📋 ПОЛНОЕ ТЕХНИЧЕСКОЕ ЗАДАНИЕ

**[Вставить сюда содержимое файла TECHNICAL_SPEC_Network_Traffic.md]**

---

### 🏗️ ТРЕБУЕМЫЕ АРТЕФАКТЫ

Для полной реализации нужны следующие файлы:

#### 1. **Kotlin Data Layer** (45% от работы)

**Файл 1: `models/NetworkStats.kt`**
- Все data classes (NetworkStats, NetworkType, PerAppTrafficStats, enum'ы)
- KDoc для каждого поля
- toString(), equals(), hashCode() implementations

**Файл 2: `datasource/NetworkStatsDataSource.kt`**
- Парсинг /proc/net/dev
- Расчет delta между двумя срезами
- Конвертация bytes → Mbps
- Обработка ошибок и edge cases
- Тесты для каждого метода

**Файл 3: `datasource/PerAppTrafficDataSource.kt`**
- Парсинг /proc/net/xt_qtaguid/stats
- Per-app трафик extraction
- App name + icon resolution
- Top N apps фильтрация

**Файл 4: `datasource/NetworkTypeDetector.kt`**
- ConnectivityManager интеграция
- Network type determination (WiFi/LTE/5G)
- Signal strength parsing
- Flow<NetworkType> observable

**Файл 5: `repository/NetworkStatsRepository.kt`**
- Interface + implementation
- Flow обработка
- Peak tracking logic
- Caching + memoization

**Файл 6: `usecase/GetNetworkStatsUseCase.kt`**
- Business logic для получения метрик
- Валидация данных
- Трансформация для UI

**Файл 7: `usecase/MonitorNetworkTrafficUseCase.kt`**
- Continuous monitoring Flow
- Сравнение с thresholds
- Alert'ы генерация

#### 2. **C++ Native Layer** (20% от работы)

**Файл 1: `CMakeLists.txt`**
- Build configuration для NDK
- Оптимизация flags
- Compilation targets

**Файл 2: `native_network_stats.h`**
- JNI function declarations
- Struct definitions

**Файл 3: `native_network_stats.cpp`**
- Реализация парсинга /proc/net/dev (C++)
- Оптимизованные вычисления
- Zero allocations на critical path
- Обработка ошибок

**Файл 4: `NativeNetworkMetrics.kt`** (JNI Bridge)
- external function declarations
- Type-safe Kotlin wrapper
- Graceful fallback на Kotlin если native недоступен

#### 3. **UI Layer** (20% от работы)

**Файл 1: `ui/network/NetworkStatsFragment.kt`**
- UI для отображения детальной статистики
- RecyclerView для per-app stats
- Pull-to-refresh

**Файл 2: `ui/overlay/NetworkOverlayView.kt`**
- Floating overlay компонент
- Display modes (Compact, Extended, Per-App, Combined)
- Real-time update логика

**Файл 3: `ui/settings/NetworkSettingsFragment.kt`**
- Настройки для трафика мониторинга
- Alert'ы конфигурация
- Display mode выбор

#### 4. **Testing Layer** (15% от работы)

**Файл 1: `test/NetworkStatsDataSourceTest.kt`**
- Unit tests для парсинга
- Mock данные из /proc/net/dev
- Edge case тесты
- Конвертация тесты

**Файл 2: `test/NetworkTypeDetectorTest.kt`**
- Тесты для network type detection
- Mock ConnectivityManager

**Файл 3: `test/NetworkStatsRepositoryTest.kt`**
- Repository business logic tests
- Flow тесты (Turbine)

**Файл 4: `test/NetworkStatsBenchmarkTest.kt`**
- Performance benchmarks
- Сравнение Kotlin vs C++
- Memory profiling

---

### 🔧 РЕАЛИЗАЦИЯ - ПОШАГОВЫЙ ПЛАН

#### ФАЗА 1: Core Data Layer (Дни 1-2)

```
Задача:
1. Создать data classes (models/NetworkStats.kt)
2. Реализовать NetworkStatsDataSource с парсингом /proc/net/dev
3. Написать unit tests
4. Убедиться что парсинг работает корректно

Acceptance Criteria:
- ✅ Data classes compile without errors
- ✅ /proc/net/dev парсинг работает
- ✅ Тесты показывают ±5% accuracy
- ✅ Нет memory leaks (LeakCanary)
```

#### ФАЗА 2: Native C++ Optimization (День 3)

```
Задача:
1. Написать native_network_stats.cpp
2. Оптимизировать парсинг через C++
3. Сделать JNI bridge (NativeNetworkMetrics.kt)
4. Benchmark Kotlin vs C++

Acceptance Criteria:
- ✅ C++ version 10x быстрее Kotlin
- ✅ JNI bridge work correctly
- ✅ Graceful fallback work
- ✅ 0 runtime errors
```

#### ФАЗА 3: Network Type Detection (День 4)

```
Задача:
1. Реализовать NetworkTypeDetector
2. Интегрировать ConnectivityManager
3. Парсить signal strength
4. Написать тесты

Acceptance Criteria:
- ✅ WiFi detection work
- ✅ LTE/5G detection work
- ✅ Signal strength парсинг work
- ✅ Тесты pass
```

#### ФАЗА 4: Per-App Traffic Analysis (День 5)

```
Задача:
1. Реализовать PerAppTrafficDataSource
2. Парсить /proc/net/xt_qtaguid/stats
3. Resolve app names + icons
4. Top N apps фильтрация

Acceptance Criteria:
- ✅ Per-app stats shows correctly
- ✅ App icons загружаются
- ✅ Top 5 apps фильтр work
- ✅ Graceful degradation без permissions
```

#### ФАЗА 5: Repository & Use Cases (День 6)

```
Задача:
1. Реализовать NetworkStatsRepository
2. Написать GetNetworkStatsUseCase
3. Написать MonitorNetworkTrafficUseCase
4. Peak tracking logic

Acceptance Criteria:
- ✅ Repository interface clean
- ✅ Use cases work independently
- ✅ Peak tracking accurate
- ✅ Flow composition work
```

#### ФАЗА 6: UI Layer (День 7-8)

```
Задача:
1. Реализовать NetworkStatsFragment
2. Реализовать NetworkOverlayView
3. Реализовать NetworkSettingsFragment
4. Integrate с existing overlay

Acceptance Criteria:
- ✅ Metrics display correctly
- ✅ Real-time updates work
- ✅ Display modes work
- ✅ Settings persist
```

#### ФАЗА 7: Testing & Optimization (День 9)

```
Задача:
1. Write comprehensive tests (unit + integration)
2. Run performance benchmarks
3. Optimize if CPU/memory overhead high
4. Code review

Acceptance Criteria:
- ✅ >80% test coverage
- ✅ CPU overhead < 1%
- ✅ Memory overhead < 20MB
- ✅ Battery impact < 0.5%/24h
```

---

### 💻 ОЖИДАЕМЫЙ КОД

Ожидаем получить production-ready код, который:

1. **Компилируется без warnings/errors**
   - Kotlin style guide compliant
   - No deprecated API usage
   - Proper null safety

2. **Работает на реальных устройствах**
   - Tested на Android 5.0+
   - Tested на разных типах сетей
   - Gracefully degradates на restricted devices

3. **Имеет полное покрытие тестами**
   - Unit tests для каждого класса
   - Integration tests для workflows
   - Benchmark tests для performance

4. **Оптимизирован по производительности**
   - <1% CPU overhead
   - <20MB RAM overhead
   - <100ms parsing cycle
   - 10x faster с C++

5. **Хорошо документирован**
   - KDoc комментарии на всех публичных APIs
   - Architecture decision records (ADR)
   - Troubleshooting guides

6. **Готов к production**
   - Proper error handling
   - Logging (Timber integration)
   - Graceful degradation при ошибках
   - No memory leaks

---

### 🎓 BEST PRACTICES

Реализация должна следовать:

**Architecture:**
- ✅ Clean Architecture (3 слоя: Presentation, Domain, Data)
- ✅ MVVM pattern для UI
- ✅ Dependency Injection (Hilt)
- ✅ Repository pattern для data access

**Code Quality:**
- ✅ SOLID principles
- ✅ DRY - Don't Repeat Yourself
- ✅ Meaningful naming
- ✅ Comments для complex logic

**Performance:**
- ✅ Coroutines для async operations
- ✅ Flow для reactive streams
- ✅ C++ для critical paths
- ✅ Caching where appropriate

**Testing:**
- ✅ Unit tests для logic
- ✅ Integration tests для workflows
- ✅ Benchmark tests для performance
- ✅ Mock external dependencies

**Documentation:**
- ✅ KDoc on all public APIs
- ✅ README с примерами
- ✅ Architecture diagrams
- ✅ Trade-offs explained

---

### 🚀 DELIVERABLES

```
network-traffic-feature/
├── kotlin/
│   ├── model/
│   │   └── NetworkStats.kt
│   ├── datasource/
│   │   ├── NetworkStatsDataSource.kt
│   │   ├── PerAppTrafficDataSource.kt
│   │   └── NetworkTypeDetector.kt
│   ├── repository/
│   │   └── NetworkStatsRepository.kt
│   ├── usecase/
│   │   ├── GetNetworkStatsUseCase.kt
│   │   └── MonitorNetworkTrafficUseCase.kt
│   ├── viewmodel/
│   │   └── NetworkStatsViewModel.kt
│   ├── ui/
│   │   ├── fragment/
│   │   │   └── NetworkStatsFragment.kt
│   │   ├── overlay/
│   │   │   └── NetworkOverlayView.kt
│   │   └── settings/
│   │       └── NetworkSettingsFragment.kt
│   └── di/
│       └── NetworkModule.kt
├── cpp/
│   ├── CMakeLists.txt
│   ├── native_network_stats.h
│   ├── native_network_stats.cpp
│   └── NativeNetworkMetrics.kt (bridge)
├── test/
│   ├── NetworkStatsDataSourceTest.kt
│   ├── NetworkTypeDetectorTest.kt
│   ├── NetworkStatsRepositoryTest.kt
│   └── NetworkStatsBenchmarkTest.kt
├── IMPLEMENTATION_NOTES.md
├── ARCHITECTURE.md
└── TROUBLESHOOTING.md
```

---

### 📊 SUCCESS CRITERIA

Реализация считается успешной когда:

✅ Все файлы скомпилированы без ошибок/warnings
✅ Real-time трафик мониторинг работает (±5% accuracy)
✅ Per-app трафик shows корректно
✅ Overlay отображает данные правильно
✅ CPU overhead < 1%, RAM < 20MB
✅ Все тесты pass (>80% coverage)
✅ Performance benchmarks достигнуты
✅ Нет memory leaks (LeakCanary)
✅ Код готов к production (code review passed)
✅ Documentation complete

---

### 🎯 TONE & STYLE

Пожалуйста, пиши код в стиле:
- **Опытного разработчика** - не объяснять базовые концепции
- **Production-ready** - готово сразу использовать
- **Хорошо структурированный** - легко понять архитектуру
- **Хорошо задокументированный** - KDoc + комментарии для сложной логики
- **Оптимизированный** - performance приоритет
- **Тестируемый** - легко писать и запускать тесты

---

### ⚙️ СПЕЦИАЛЬНЫЕ ТРЕБОВАНИЯ

1. **Для парсинга /proc/net/dev:**
   - Используй разницу между двумя срезами (snapshot pattern)
   - Обработай интерфейсы которые появляются/исчезают
   - Ignore loopback интерфейс (lo)

2. **Для per-app трафика:**
   - Gracefully handle если /proc/net/xt_qtaguid/stats недоступен
   - Кэшируй app информацию (name, icon) чтобы не перепарсить
   - Поддержи как root так и non-root devices

3. **Для C++ оптимизации:**
   - Используй stack allocations вместо heap
   - Минимизируй string копирования
   - Используй mmapped файлы если возможно
   - Сравни performance с Kotlin в benchmarks

4. **Для UI:**
   - Animate transitions между display modes
   - Используй Material Design 3
   - Поддержи dark mode
   - Responsive layout для разных screen sizes

---

### 📞 QUESTIONS TO CLARIFY

Если возникают вопросы во время реализации:

1. **Архитектурные вопросы** → Ищи в TECHNICAL_SPEC_Network_Traffic.md section 3
2. **Performance вопросы** → Ищи в section 9 (Performance Targets)
3. **API вопросы** → Ищи в section 6 (Android APIs)
4. **Error handling** → Ищи в section 7 (Error Handling)

Если ответа нет → Make reasonable decisions based on best practices.

---

## ФИНАЛЬНАЯ ИНСТРУКЦИЯ

Прямо сейчас:
1. Внимательно прочитай TECHNICAL_SPEC_Network_Traffic.md (все 14 разделов)
2. Убедись что ты понимаешь всю архитектуру и требования
3. Начни с ФАЗЫ 1 (Core Data Layer) - Day 1-2
4. Следуй пошаговому плану фаз
5. После каждой фазы проверь Acceptance Criteria
6. Если что-то непонятно - вернись к ТЗ и перечитай
7. В конце предоставь все deliverables в папке network-traffic-feature/

**Основной принцип:** Лучше написать меньше кода но отличного качества, чем много кода с проблемами.

---

**Ты готов? Давай начнем! 🚀**

---

