# 🔴 СРОЧНО: CPU Показывает 0% - Диагностика

**Date:** 2025-12-10 14:08  
**Problem:** CPU всегда 0%, /proc/stat возвращает нули  
**Status:** 🔧 В процессе диагностики  

---

## 🐛 Обнаруженные Проблемы

### 1. CPU Stats = 0

**Из ваших логов:**
```
📊 Current CPU stats: total=0, active=0, idle=0
⚠️ Baseline not initialized, initializing now...
⏳ First reading stored as baseline, returning 0%
```

**Проблема:** `/proc/stat` читается, но парсинг возвращает все нули.

**Возможные причины:**
1. Файл `/proc/stat` имеет другой формат на вашем Android
2. Парсинг не работает корректно
3. Права доступа ограничены (SELinux)

---

### 2. Только 1 Процесс

**Из ваших логов:**
```
📱 Found 1 running processes
📊 Collected 0 user apps with measurable usage
```

**Проблема:** `ActivityManager.runningAppProcesses` на Android 10+ ограничен по соображениям приватности и возвращает только процессы самого приложения.

**Решение:** Нужно использовать `UsageStatsManager` (требует разрешение PACKAGE_USAGE_STATS).

---

## 🔧 Срочные Исправления (ВНЕДРЕНЫ)

### Исправление 1: Детальное Логирование /proc/stat

Добавлено в `SystemDataSource.kt`:

**Новые логи с тегом `SYS_DATA`:**
- 📁 Путь к файлу
- ❌ Проверка существования
- ❌ Проверка прав чтения
- 📝 Содержимое первой строки
- 📦 Результат парсинга
- ❌ Детекция нулевого результата

### Исправление 2: Детальное Логирование Парсинга

Добавлено в `MetricsParser.kt`:

**Новые логи с тегом `PARSER`:**
- 🔍 Входящая строка
- 📦 Разделённые части
- ❌ Проверка количества частей
- ⚠️ Проверка формата
- ✅ Результат парсинга

---

## 🚀 Команды для Диагностики

### Пересобрать и Установить

```bash
cd /home/tester/CascadeProjects/SysMetrics
./gradlew clean installDebug
```

### Запустить с Новыми Логами

```bash
# Очистить старые логи
adb logcat -c

# Запустить приложение
adb shell am start -n com.sysmetrics.app/.ui.MainActivity

# Смотреть детальную диагностику
adb logcat -s SYS_DATA:V PARSER:V METRICS_CPU:D
```

---

## 📊 Ожидаемый Вывод

### Если /proc/stat читается ПРАВИЛЬНО:

```
SYS_DATA: 📁 Reading CPU stats from: /proc/stat
SYS_DATA: 📝 Raw /proc/stat line: 'cpu  123456 0 789012 3456789 ...'
PARSER: 🔍 Parsing CPU line (length=XX): 'cpu  123456 0 789012...'
PARSER: 📦 Split into 11 parts: [cpu, 123456, 0, 789012, 3456789, ...]
PARSER: ✅ Parsed: user=123456, nice=0, system=789012, idle=3456789, ...
SYS_DATA: 📦 Parsed CpuStats: total=XXXXX, user=123456, system=789012, idle=3456789
```

### Если /proc/stat ПУСТОЙ или НЕПРАВИЛЬНЫЙ:

```
SYS_DATA: 📁 Reading CPU stats from: /proc/stat
SYS_DATA: ❌ /proc/stat does NOT exist!
```

ИЛИ

```
SYS_DATA: 📁 Reading CPU stats from: /proc/stat
SYS_DATA: ❌ /proc/stat exists but CANNOT READ (permission denied?)
```

ИЛИ

```
SYS_DATA: 📝 Raw /proc/stat line: ''  ← ПУСТАЯ СТРОКА
PARSER: ❌ Insufficient parts: 0 (need at least 8)
```

ИЛИ

```
SYS_DATA: 📝 Raw /proc/stat line: 'some unexpected format'
PARSER: ❌ Insufficient parts: 2 (need at least 8)
```

---

## 🔍 Проверка Вручную (на устройстве)

### 1. Проверить существование /proc/stat

```bash
adb shell "ls -l /proc/stat"
```

**Ожидается:**
```
-r--r--r-- 1 root root 0 2025-12-10 14:00 /proc/stat
```

### 2. Проверить содержимое /proc/stat

```bash
adb shell "head -1 /proc/stat"
```

**Ожидается (нормальный формат):**
```
cpu  123456 789 234567 8901234 12345 0 6789 0 0 0
```

**Если вывод другой** - сообщите мне точный формат!

### 3. Проверить несколько раз подряд

```bash
adb shell "cat /proc/stat | head -1; sleep 1; cat /proc/stat | head -1"
```

Значения **ДОЛЖНЫ ОТЛИЧАТЬСЯ** между двумя чтениями!

---

## 🛠️ Возможные Решения

### Решение 1: SELinux блокирует доступ

Если `/proc/stat` недоступен из-за SELinux:

```bash
# Проверить SELinux status
adb shell getenforce

# Временно отключить (для теста, требует root)
adb shell su -c setenforce 0
```

### Решение 2: Использовать Native Code (JNI)

Native C++ код может иметь больше прав для чтения `/proc/stat`:

```cpp
// app/src/main/cpp/native_metrics.cpp
extern "C" JNIEXPORT jlong JNICALL
Java_com_sysmetrics_app_NativeMetrics_readCpuStats(JNIEnv* env, jclass) {
    FILE* f = fopen("/proc/stat", "r");
    if (!f) return 0;
    
    long user, nice, system, idle, iowait, irq, softirq;
    fscanf(f, "cpu %ld %ld %ld %ld %ld %ld %ld",
           &user, &nice, &system, &idle, &iowait, &irq, &softirq);
    fclose(f);
    
    return user + nice + system + idle + iowait + irq + softirq;
}
```

### Решение 3: Fallback на /proc/loadavg

Если `/proc/stat` не работает, можно использовать `/proc/loadavg`:

```bash
adb shell "cat /proc/loadavg"
# Output: 1.23 2.45 3.67 4/567 8901
# Первые 3 числа - load average за 1, 5, 15 минут
```

### Решение 4: Использовать ActivityManager для общей CPU

```kotlin
val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
val memInfo = ActivityManager.MemoryInfo()
activityManager.getMemoryInfo(memInfo)
// memInfo.availMem, memInfo.totalMem - но нет CPU!
```

---

## 📞 Следующие Шаги

### ШАГ 1: Пересобрать с новыми логами

```bash
cd /home/tester/CascadeProjects/SysMetrics
./gradlew clean installDebug
```

### ШАГ 2: Собрать новые логи

```bash
adb logcat -c
adb shell am force-stop com.sysmetrics.app
adb shell am start -n com.sysmetrics.app/.ui.MainActivity
adb logcat -s SYS_DATA:V PARSER:V > diagnostic_logs.txt
```

Подождите 5 секунд, нажмите Ctrl+C.

### ШАГ 3: Проверить /proc/stat вручную

```bash
adb shell "head -5 /proc/stat"
```

Скопируйте вывод и отправьте мне.

### ШАГ 4: Прислать результаты

Мне нужны:
1. **diagnostic_logs.txt** - новые логи с тегами SYS_DATA и PARSER
2. **Вывод команды** `adb shell "head -5 /proc/stat"`
3. **Android версия**: `adb shell getprop ro.build.version.release`
4. **Устройство**: `adb shell getprop ro.product.model`

---

## 🎯 Временное Решение (если /proc/stat не работает)

Пока мы выясняем проблему, можно временно отображать:
- ✅ RAM usage (работает)
- ❌ CPU = "N/A" (не работает)
- ✅ Self memory (работает)
- ❌ Top apps (не работает из-за Android 10+ ограничений)

Но это НЕ решение - нужно разобраться почему `/proc/stat` не читается!

---

## 📚 Полезные Ссылки

**Формат /proc/stat:**
```
cpu  user nice system idle iowait irq softirq steal guest guest_nice
cpu0 ...
cpu1 ...
```

**Документация:**
- https://www.kernel.org/doc/Documentation/filesystems/proc.txt
- https://man7.org/linux/man-pages/man5/proc.5.html

---

**Status:** 🔧 Ожидаю новые логи с тегами SYS_DATA и PARSER  
**Next:** Пересобрать, запустить, прислать результаты  

**Created:** 2025-12-10 14:08:42+03:00  
**Engineer:** Senior Android Developer
