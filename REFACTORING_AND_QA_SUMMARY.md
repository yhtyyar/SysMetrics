# Refactoring & QA Summary

**Version:** 1.5.0 → 1.5.1  
**Date:** December 15, 2025  
**Performed by:** Android Tech Lead  
**Status:** ✅ Completed  

---

## 📋 Executive Summary

Проведён технический рефакторинг кода и подготовка к QA тестированию:
- ✅ **Исправлен TODO** - реализовано сохранение позиции overlay
- ✅ **Создана QA документация** - чеклист, шаблон багов, руководство
- ✅ **Собрана debug сборка** - готова к тестированию
- ✅ **Подготовлены инструкции** - как сообщать о багах

---

## 🔧 Code Refactoring

### Изменённые файлы

#### 1. MinimalistOverlayService.kt

**Проблема:** TODO - позиция overlay не сохранялась

**Исправление:**
```kotlin
// До:
onPositionChanged = { x, y ->
    Timber.tag(TAG_SERVICE).d("Overlay position saved: ($x, $y)")
    // TODO: Save position to preferences for persistence
}

// После:
@Inject
lateinit var preferencesDataSource: PreferencesDataSource

onPositionChanged = { x, y ->
    Timber.tag(TAG_SERVICE).d("Overlay position changed: ($x, $y)")
    lifecycleScope.launch {
        preferencesDataSource.updatePosition(x, y)
        Timber.tag(TAG_SERVICE).i("✅ Position saved to preferences")
    }
}
```

**Преимущества:**
- ✅ Позиция overlay теперь сохраняется в DataStore
- ✅ Используется lifecycle-aware coroutine scope
- ✅ Асинхронная операция не блокирует UI
- ✅ Логирование подтверждает сохранение

**Тестирование:**
1. Передвинуть overlay на мобильном устройстве
2. Остановить сервис
3. Запустить снова
4. **Ожидается:** Overlay появится на сохранённой позиции

---

## 📚 QA Documentation Created

### 1. QA_TESTING_CHECKLIST.md

**Назначение:** Подробный чеклист для систематического тестирования

**Содержание:**
- ✅ Pre-Testing Setup (7 пунктов)
- ✅ Installation & Permissions (6 пунктов)
- ✅ Core Functionality - Overlay Display (15 пунктов)
- ✅ UI/UX - Overlay Appearance (10 пунктов)
- ✅ Performance & Stability (12 пунктов)
- ✅ Lifecycle & State Management (12 пунктов)
- ✅ Settings & Configuration (6 пунктов)
- ✅ Device Compatibility (10 пунктов)
- ✅ Error Handling (9 пунктов)
- ✅ Logging & Debugging (7 пунктов)
- ✅ Regression Testing (5 пунктов)

**Всего:** 99 тестовых пунктов

**Как использовать:**
```bash
# Открыть чеклист
open QA_TESTING_CHECKLIST.md

# Заполнять чекбоксы по мере тестирования
- [x] Пройденный тест
- [ ] Непройденный тест
```

---

### 2. BUG_REPORT_TEMPLATE.md

**Назначение:** Стандартизированный формат для репортинга багов

**Секции:**
1. **Bug Information** - Title, Severity, Priority, Component
2. **Environment** - Device, Android version, App version
3. **Reproduction Steps** - Детальные шаги для воспроизведения
4. **Expected vs Actual** - Что должно vs что происходит
5. **Evidence** - Screenshots, LogCat output
6. **Analysis** - Root cause, affected code
7. **Workaround** - Временное решение
8. **Verification** - Как проверить фикс

**Severity Levels:**
- 🔴 **Critical** - Crash, data loss, core broken
- 🟡 **High** - Major feature broken
- 🟢 **Medium** - Minor feature broken
- ⚪ **Low** - Cosmetic issue

**Как использовать:**
```bash
# Скопировать шаблон
cp BUG_REPORT_TEMPLATE.md bugs/BUG-001-cpu-zero.md

# Заполнить все секции
# Приложить логи и скриншоты
# Отправить в issue tracker
```

---

### 3. QA_TESTING_GUIDE.md

**Назначение:** Полное руководство по тестированию для QA инженеров

**Содержание:**
1. **Overview** - Что такое SysMetrics, что тестировать
2. **Setup & Prerequisites** - ADB, device setup
3. **Installation** - Как установить APK
4. **Core Testing Scenarios** (6 сценариев):
   - Test 1: First Launch & Permissions
   - Test 2: CPU Monitoring Accuracy
   - Test 3: RAM Monitoring
   - Test 4: Top Apps Tracking
   - Test 5: Overlay Position & Dragging
   - Test 6: Lifecycle & State Management
5. **LogCat Monitoring** - Команды, теги, интерпретация
6. **Common Issues & Solutions** - Troubleshooting guide
7. **Performance Testing** - Memory, CPU, Battery
8. **Bug Reporting** - Когда и как репортить

**Объём:** 500+ строк подробных инструкций

**Ключевые команды:**
```bash
# Мониторинг всех логов SysMetrics
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_"

# Проверка CPU
adb logcat -s METRICS_CPU:D METRICS_BASELINE:D

# Проверка Top Apps
adb logcat -s PROC_TOP:D

# Сохранение логов
adb logcat -d > sysmetrics_logs.txt
```

---

## 📦 Debug Build

### Build Configuration

**Gradle Command:**
```bash
./gradlew clean assembleDebug
```

**Build Type:** Debug  
**Minify:** Disabled  
**Debuggable:** True  
**LeakCanary:** Enabled  

**Output Location:**
```
app/build/outputs/apk/debug/app-debug.apk
```

### Debug Features Enabled

1. **LeakCanary**
   - Автоматическое обнаружение memory leaks
   - Notification при обнаружении утечки
   - Детальный trace leak path

2. **Timber Logging**
   - Debug logs enabled
   - Structured tags for filtering
   - Colour-coded output

3. **BuildConfig.DEBUG**
   - Debug-specific code paths active
   - Extra validation enabled

### Installation Instructions

**Method 1: ADB**
```bash
# Установка
adb install app/build/outputs/apk/debug/app-debug.apk

# Переустановка (если уже установлено)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Запуск
adb shell am start -n com.sysmetrics.app/.ui.MainActivity
```

**Method 2: Manual**
1. Скопировать APK на устройство
2. Включить "Unknown sources" в настройках
3. Открыть файл и нажать Install

**Verification:**
```bash
# Проверить установку
adb shell pm list packages | grep sysmetrics
# Output: package:com.sysmetrics.app

# Проверить версию
adb shell dumpsys package com.sysmetrics.app | grep versionName
# Output: versionName=1.5.0
```

---

## 🐛 How to Report Bugs

### Quick Guide

**Когда репортить:**
- ❌ Crash (приложение закрывается)
- ❌ ANR (приложение зависает)
- ❌ Неправильные данные (CPU 0%, RAM >100%)
- ❌ UI проблемы (текст обрезан, наложение)
- ❌ Memory leak (LeakCanary notification)
- ❌ Функциональность не работает

**Что включить в репорт:**

1. **Базовая информация**
   ```
   Title: "CPU shows 0% on Android 13"
   Severity: Critical
   Device: Samsung Galaxy S21
   Android: 13 (API 33)
   ```

2. **Шаги для воспроизведения**
   ```
   1. Открыть приложение
   2. Нажать "Start Monitor"
   3. Подождать 3 секунды
   4. Наблюдать CPU = 0%
   ```

3. **Логи**
   ```bash
   # Воспроизвести баг, затем:
   adb logcat -d > bug_cpu_zero.txt
   ```

4. **Screenshots**
   - Скриншот проблемы
   - Скриншот ожидаемого поведения (если есть)

### Detailed Process

**Шаг 1: Воспроизвести**
- Убедиться что баг стабильно воспроизводится
- Записать точные шаги
- Отметить частоту (Always, Often, Sometimes, Rare)

**Шаг 2: Собрать данные**
```bash
# Очистить логи
adb logcat -c

# Воспроизвести баг

# Сохранить логи
adb logcat -d > bug_$(date +%Y%m%d_%H%M%S).txt
```

**Шаг 3: Заполнить шаблон**
1. Копировать `BUG_REPORT_TEMPLATE.md`
2. Переименовать: `BUG-XXX-short-description.md`
3. Заполнить все обязательные секции
4. Приложить логи и screenshots

**Шаг 4: Отправить**
- GitHub Issues (если используется)
- JIRA ticket (если настроен)
- Email с пометкой [BUG]

---

## 🎯 Testing Priorities

### Critical (Must Test) 🔴
1. **Installation & Permissions**
2. **Overlay Display**
3. **CPU Monitoring Accuracy**
4. **RAM Monitoring**
5. **No Crashes**
6. **No Memory Leaks**

### High Priority 🟡
1. **Top Apps Tracking**
2. **Lifecycle Management**
3. **Performance (CPU, RAM, Battery)**
4. **Adaptive Intervals**

### Medium Priority 🟢
1. **Overlay Position Persistence** (NEW!)
2. **Settings**
3. **Device Compatibility**
4. **UI/UX Polish**

### Low Priority ⚪
1. **Edge Cases**
2. **Cosmetic Issues**
3. **Nice-to-have Features**

---

## 📊 Expected Test Results

### Passing Criteria

**Functional:**
- ✅ All critical features work
- ✅ CPU shows 5-95% (not 0%)
- ✅ RAM shows realistic values
- ✅ Top apps displayed correctly
- ✅ Overlay draggable on mobile
- ✅ Position saves/restores (mobile)

**Performance:**
- ✅ Memory < 50MB
- ✅ CPU overhead < 5%
- ✅ No frame drops
- ✅ Battery drain < 2%/hour

**Stability:**
- ✅ No crashes in 10-minute session
- ✅ No ANRs
- ✅ No memory leaks (LeakCanary)
- ✅ Survives lifecycle events

**Logging:**
- ✅ All log tags present
- ✅ No error logs
- ✅ "Baseline initialized" logged
- ✅ "Using NATIVE JNI" logged

### Known Issues

**Android 10+ CPU Reading:**
- **Issue:** Android 10+ restricts /proc/stat access
- **Solution:** Native JNI bypass implemented
- **Expected:** Logs show "Using NATIVE JNI"
- **Fallback:** Kotlin implementation if native fails

**Android TV Hover Events:**
- **Issue:** TV remote hover can crash app
- **Solution:** Exception handler implemented
- **Expected:** No crash, hover events logged

---

## 📋 QA Workflow

### Day 1: Setup & Smoke Test
1. Install debug APK
2. Verify ADB connection
3. Run smoke test (15 min)
   - Launch app
   - Start overlay
   - Verify metrics display
   - Stop overlay

### Day 2: Functional Testing
1. Use `QA_TESTING_CHECKLIST.md`
2. Test all critical features
3. Test high priority features
4. Document any bugs using template

### Day 3: Performance & Stress Testing
1. Memory usage test (1 hour)
2. CPU overhead test
3. Battery drain test (if time allows)
4. Stress test (rapid start/stop)

### Day 4: Compatibility Testing
1. Test on different Android versions
2. Test on different devices
3. Test on TV (if available)
4. Document compatibility matrix

### Day 5: Bug Verification & Sign-off
1. Verify all reported bugs
2. Retest critical scenarios
3. Final smoke test
4. Sign-off checklist

---

## 🔗 Document Links

**For QA Testers:**
- 📋 [QA_TESTING_CHECKLIST.md](QA_TESTING_CHECKLIST.md) - Main checklist
- 📖 [QA_TESTING_GUIDE.md](QA_TESTING_GUIDE.md) - Detailed guide
- 🐛 [BUG_REPORT_TEMPLATE.md](BUG_REPORT_TEMPLATE.md) - Bug template

**For Developers:**
- 📚 [DEVELOPMENT.md](DEVELOPMENT.md) - Development guide
- 📝 [REQUIREMENTS.md](REQUIREMENTS.md) - Requirements
- 📊 [CHANGELOG.md](CHANGELOG.md) - Version history
- 🔍 [LOGGING_GUIDE.md](LOGGING_GUIDE.md) - Logging reference

**For Product:**
- 📖 [README.md](README.md) - Project overview

---

## ✅ Checklist: Ready for QA

- [x] Code refactoring completed
- [x] TODO items resolved
- [x] Debug build compiled
- [x] QA checklist created
- [x] Bug report template created
- [x] Testing guide created
- [x] LogCat commands documented
- [x] Known issues documented
- [x] Installation instructions provided
- [x] Build verification passed

**Status:** ✅ **READY FOR QA TESTING**

---

## 📞 Contact

**Questions about testing?**  
Contact: Android Tech Lead

**Found a bug?**  
Use: `BUG_REPORT_TEMPLATE.md`

**Need help with logs?**  
See: `LOGGING_GUIDE.md`

**Technical questions?**  
See: `DEVELOPMENT.md`

---

## 🎓 Tips for Effective Testing

1. **Always use LogCat** - Don't test blind
2. **Test one thing at a time** - Easier to isolate bugs
3. **Document everything** - Screenshots + logs
4. **Reproduce before reporting** - Confirm it's reproducible
5. **Provide context** - Device, Android version, steps
6. **Use templates** - Consistent bug reports
7. **Test edge cases** - Rotate, minimize, low memory
8. **Check LeakCanary** - Memory leaks are bugs too
9. **Time your tests** - Note performance issues
10. **Think like a user** - UX problems are bugs

---

**Good luck with testing! 🚀**

*This document prepared by Android Tech Lead on December 15, 2025*
