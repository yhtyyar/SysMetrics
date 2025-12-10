# 🚀 Быстрый Старт: Мониторинг SysMetrics

## Для Немедленного Использования

### 1. Проверить Что Показывается на Экране ATV

```bash
adb logcat -s OVERLAY_DISPLAY:D
```

**Вы увидите:**
```
📺 CPU on SCREEN: 'CPU: 45%' color=GREEN
📺 RAM on SCREEN: 'RAM: 1234/2048 MB' (60.3%)
📺 SELF on SCREEN: 'Self: 1.5% / 42M'
📺   #1: YouTube: 23% / 567MB
📺   #2: Chrome: 12% / 234MB
```

---

### 2. Отследить Всю Работу Программы

```bash
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_"
```

**Это покажет:**
- ✅ Запуск сервиса
- 📊 Сбор метрик
- 📺 Что на экране
- 🏆 Топ приложений
- ⚠️ Предупреждения

---

### 3. Проверить Расчёты CPU (если CPU = 0%)

```bash
adb logcat -s METRICS_CPU:D METRICS_BASELINE:I
```

**Ищите:**
- ✅ `CPU baseline initialized` - baseline создан
- 📈 `CPU: totalΔ=645 → 44.5%` - есть delta
- ⚠️ `Zero or negative totalΔ` - проблема!

---

### 4. Только Ошибки

```bash
adb logcat -s METRICS_ERROR:E PROC_ERROR:E
```

**Если нет вывода** → всё работает ✅

---

## Полная Документация

- 📖 **[LOGGING_GUIDE.md](LOGGING_GUIDE.md)** - Полное руководство (450+ строк)
- 🔧 **[CPU_FIX_ANALYSIS.md](CPU_FIX_ANALYSIS.md)** - Анализ проблемы с CPU
- 📋 **[IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)** - Итоговый отчёт
- 📄 **[LOGGING_TAGS_REFERENCE.txt](LOGGING_TAGS_REFERENCE.txt)** - Справка по тегам

---

## Типовые Проблемы

### ❌ CPU показывает 0%

```bash
# Проверить baseline
adb logcat -s METRICS_BASELINE:D METRICS_CPU:D

# Ожидается:
# ✅ CPU baseline initialized
# 📈 CPU: totalΔ > 0
```

**Если totalΔ = 0:** Подождите 2 секунды после запуска

---

### ❌ Нет топ-приложений

```bash
# Проверить сбор
adb logcat -s PROC_TOP:D

# Ожидается:
# 🏆 #1: AppName: X% / YMB
```

**Если "Collected 0 user apps":** Запустите YouTube/Chrome

---

### ❌ Overlay не появляется

```bash
# Проверить создание
adb logcat -s OVERLAY_SERVICE:I

# Ожидается:
# ✅ Overlay view created and added to window
```

**Если нет:** Проверьте разрешение `SYSTEM_ALERT_WINDOW`

---

## Контакты

- **GitHub:** https://github.com/yhtyyar/SysMetrics
- **Документация:** См. LOGGING_GUIDE.md
- **Issues:** GitHub Issues

Создано: 2025-12-10 ✨
