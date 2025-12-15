# ТЕХНИЧЕСКОЕ ЗАДАНИЕ (ТЗ)
## SysMetrics Pro - Upgrade & Enhancement

**Версия:** 2.0  
**Дата:** 15.12.2025  
**Статус:** Ready for Implementation  

---

## 📋 ОГЛАВЛЕНИЕ

1. [Обзор проекта](#обзор-проекта)
2. [Текущее состояние](#текущее-состояние)
3. [Требуемые улучшения](#требуемые-улучшения)
4. [Технические спецификации](#технические-спецификации)
5. [Архитектура](#архитектура)
6. [Критерии завершения](#критерии-завершения)

---

## 🎯 ОБЗОР ПРОЕКТА

### Что это?
**SysMetrics** — высокопроизводительное Android-приложение для мониторинга системных ресурсов (CPU, RAM, температура) в реальном времени.

### Характеристики
- ✅ MVVM архитектура + Clean Architecture
- ✅ Floating overlay для real-time мониторинга
- ✅ CPU, RAM, температура мониторинг
- ✅ <50MB памяти оптимизация
- ✅ Native Kotlin JNI bridge
- ✅ Benchmark тесты

---

## 📊 ТЕКУЩЕЕ СОСТОЯНИЕ

### Что работает ✅
- Мониторинг CPU в реальном времени
- Мониторинг RAM
- Floating overlay виджет
- Оптимизированная производительность
- Native метрики через JNI

### Что НЕ работает ❌
- Self vs Other apps разделение процессов
- 24-hour история метрик (SQLite)
- Экспорт данных (CSV/JSON)
- Material 3 UI компоненты
- Детальный анализ памяти по типам
- Settings экран
- Фоновый сервис мониторинга

---

## 🔧 ТРЕБУЕМЫЕ УЛУЧШЕНИЯ

### 1. Process Segmentation (CRITICAL)

#### Цель
Разделить метрики собственного приложения от других приложений

#### Требования
- ✅ Получать список процессов через `ActivityManager`
- ✅ Определять Self по PID и package name
- ✅ Классифицировать Other приложения (system/user)
- ✅ 95% точность метрик
- ✅ <50ms выполнение для 300+ процессов

#### Возвращаемые данные
```
ProcessSegmentation {
  selfProcesses: [
    ProcessInfo { pid, packageName, processName, memoryBytes, cpuPercent, state }
  ],
  otherProcesses: [
    ProcessInfo { ... }
  ],
  timestamp: Long
}
```

---

### 2. Detailed Memory Analysis (HIGH)

#### Цель
Показать breakdown памяти по типам

#### Требования
- ✅ Breakdown: Native, Java Heap, Graphics, Stack, Other
- ✅ Точность ±5% от Debug.MemoryInfo
- ✅ Self + top 10 других приложений
- ✅ Update каждые 2-3 сек

#### Структура данных
```
DetailedProcessMemory {
  pid: Int,
  packageName: String,
  memoryBreakdown: MemoryBreakdown {
    nativeMemory: Long,
    javaHeap: Long,
    graphicsMemory: Long,
    stackMemory: Long,
    other: Long
  },
  totalPss: Long
}
```

---

### 3. Room Database для истории (HIGH)

#### Цель
Сохранять историю метрик за 24 часа

#### Требования
- ✅ SQLite через Room ORM
- ✅ Сохранять каждые 2-3 сек
- ✅ Auto-cleanup данных старше 24h
- ✅ Max 50MB на диск (оптимизация)
- ✅ ~30k записей в день

#### Сущности
```
MetricsEntity {
  id: Long (PK),
  timestamp: Long,
  cpuPercent: Float,
  ramPercent: Float,
  temperature: Float,
  createdAt: Long
}

ProcessEntity {
  id: Long (PK),
  pid: Int,
  packageName: String,
  cpuPercent: Float,
  memoryBytes: Long,
  timestamp: Long
}
```

---

### 4. CSV/JSON Export (MEDIUM)

#### Цель
Экспортировать метрики для анализа

#### Требования
- ✅ Export последних 24h метрик
- ✅ CSV формат (timestamp, cpu, ram, temp)
- ✅ JSON формат (structured)
- ✅ Save в Downloads folder
- ✅ Share через intent

#### CSV пример
```
timestamp,cpu_percent,ram_percent,temperature
1702650240000,42.5,65.2,45.3
1702650242000,38.2,64.1,45.1
```

#### JSON пример
```json
{
  "exportDate": "2025-12-15T16:50:00Z",
  "metrics": [
    {
      "timestamp": 1702650240000,
      "cpu": 42.5,
      "ram": 65.2,
      "temperature": 45.3
    }
  ]
}
```

---

### 5. Material 3 UI Update (MEDIUM)

#### Цель
Обновить интерфейс на Material 3

#### Требования
- ✅ Material 3 color scheme
- ✅ Dark mode поддержка
- ✅ Responsive layout (phone/tablet)
- ✅ Smooth animations (250-300ms)

#### Color palette
```
Light:
- primary: #2180A0
- onPrimary: #FFFFFF
- primaryContainer: #B1E5FF
- secondary: #5E5260
- tertiary: #7D5260

Dark:
- primary: #A8D1E5
- onPrimary: #134252
```

---

### 6. Settings Screen (MEDIUM)

#### Цель
Полностью реализовать экран настроек

#### Требования
- ✅ Update interval (1-5 сек, default 2)
- ✅ Overlay toggle
- ✅ Background service toggle
- ✅ Theme selector (Light/Dark/Auto)
- ✅ Data export button
- ✅ Clear history button

#### UI Layout
```
SETTINGS
├─ Monitoring
│  ├─ Update Interval [2s]
│  ├─ Overlay Enabled [✓]
│  └─ Background Service [✓]
├─ Appearance
│  └─ Theme [∨ Auto]
├─ Data
│  ├─ Export Metrics
│  └─ Clear History
└─ About
   └─ Version 1.0
```

---

### 7. Background Service (LOW)

#### Цель
Продолжать мониторинг в фоне

#### Требования
- ✅ Foreground Service с notification
- ✅ WorkManager для periodic tasks
- ✅ Low battery optimization (5-10 сек)
- ✅ <2% battery drain per hour

---

## 🏗️ ТЕХНИЧЕСКИЕ СПЕЦИФИКАЦИИ

### Architecture Pattern
```
Presentation (MVVM)
    ↓
Domain (UseCase)
    ↓
Repository
    ↓
Data Layer
├─ Room Database
├─ System APIs (/proc/stat)
├─ ActivityManager
└─ Preferences
```

### Technology Stack
```
Language: Kotlin 1.9+
Framework: Android 8.0+ (API 26+)
DI: Hilt 2.48+
Database: Room 2.6+
Async: Coroutines 1.7+ + Flow
UI: Material 3
Testing: JUnit 4 + MockK
```

### Performance Requirements
```
Metrics updates: <16ms (60 FPS)
Process parsing: <50ms
Memory usage: <150MB
Battery drain: <2% per hour
Startup time: <2 sec
```

### Constraints
```
Min Android: 8.0 (API 26)
Max Android: 15 (API 35)
Work on low-end devices (2GB RAM)
<5% CPU usage при мониторинге
Responsive на phone/tablet
```

---

## 🎯 АРХИТЕКТУРА

### Directory Structure
```
app/src/main/kotlin/com/example/sysmetrics/
├── data/
│   ├── datasource/
│   │   ├── ProcessDataSource.kt (NEW)
│   │   ├── MemoryDataSource.kt (NEW)
│   │   └── SystemMetricsDataSource.kt
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt (NEW)
│   │   │   ├── entity/
│   │   │   │   ├── MetricsEntity.kt (NEW)
│   │   │   │   └── ProcessEntity.kt (NEW)
│   │   │   └── dao/
│   │   │       └── MetricsDao.kt (NEW)
│   │   └── preferences/
│   │       └── PreferencesManager.kt
│   └── repository/
│       ├── ProcessRepositoryImpl.kt (UPDATE)
│       ├── HistoryRepositoryImpl.kt (NEW)
│       └── ExportRepositoryImpl.kt (NEW)
├── domain/
│   ├── repository/
│   │   ├── IProcessRepository.kt (UPDATE)
│   │   ├── IHistoryRepository.kt (NEW)
│   │   └── IExportRepository.kt (NEW)
│   ├── usecase/
│   │   ├── GetProcessListUseCase.kt (NEW)
│   │   ├── GetMemoryAnalysisUseCase.kt (NEW)
│   │   └── ExportMetricsUseCase.kt (NEW)
│   └── model/
│       ├── ProcessInfo.kt (UPDATE)
│       ├── MemoryBreakdown.kt (NEW)
│       └── ProcessSegmentation.kt (NEW)
├── ui/
│   ├── screens/
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt
│   │   │   └── DashboardViewModel.kt
│   │   ├── details/
│   │   │   ├── DetailsScreen.kt
│   │   │   └── DetailsViewModel.kt
│   │   └── settings/
│   │       ├── SettingsScreen.kt (NEW)
│   │       └── SettingsViewModel.kt (NEW)
│   ├── components/
│   │   ├── CpuChart.kt (NEW)
│   │   ├── RamChart.kt (NEW)
│   │   └── MetricsCard.kt
│   └── theme/
│       ├── Theme.kt (UPDATE)
│       ├── Color.kt (UPDATE)
│       └── Typography.kt
├── service/
│   ├── MetricsBackgroundService.kt (NEW)
│   └── MetricsWorker.kt (NEW)
├── utils/
│   ├── exporters/
│   │   ├── CsvExporter.kt (NEW)
│   │   └── JsonExporter.kt (NEW)
│   └── formatters/
│       └── DataFormatter.kt
└── di/
    └── AppModule.kt (UPDATE)
```

---

## ✅ КРИТЕРИИ ЗАВЕРШЕНИЯ

### Функциональность
- [ ] Process segmentation (Self vs Other) работает
- [ ] Memory breakdown по всем типам
- [ ] 24h история в БД
- [ ] CSV/JSON экспорт
- [ ] Settings сохраняются

### Качество
- [ ] 70%+ test coverage
- [ ] Все unit tests passing
- [ ] Performance benchmarks OK
- [ ] No memory leaks
- [ ] Code passes detekt/ktlint

### Performance
- [ ] Metrics updates: <16ms
- [ ] Process parsing: <50ms
- [ ] Memory: <150MB
- [ ] Battery: <2%/hour
- [ ] Startup: <2 sec

### Documentation
- [ ] KDoc comments
- [ ] Architecture diagrams
- [ ] README updated
- [ ] API documentation
- [ ] Testing guide

---

## 📅 TIMELINE

**День 1-2:** Process Segmentation  
**День 3:** Detailed Memory Analysis  
**День 4:** Room Database + Export  
**День 5:** Material 3 UI + Settings  
**День 6:** Charts и Polish  
**День 7:** Testing & Optimization  
**День 8:** Documentation & Release  

---

## 📞 КОНТАКТЫ

**GitHub:** https://github.com/yhtyyar/SysMetrics  
**Min API:** 26 (Android 8.0)  
**Target API:** 34 (Android 14)

---

*Документ готов к использованию в разработке!* ✅
