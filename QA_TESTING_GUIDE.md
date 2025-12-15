# QA Testing Guide - SysMetrics Pro

**Version:** 1.5.0  
**For:** QA Engineers & Testers  
**Last Updated:** December 15, 2025  

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Setup & Prerequisites](#setup--prerequisites)
3. [Installation](#installation)
4. [Core Testing Scenarios](#core-testing-scenarios)
5. [LogCat Monitoring](#logcat-monitoring)
6. [Common Issues & Solutions](#common-issues--solutions)
7. [Performance Testing](#performance-testing)
8. [Bug Reporting](#bug-reporting)

---

## 🎯 Overview

### What is SysMetrics Pro?

SysMetrics Pro is a real-time Android system monitoring application that displays:
- **CPU usage** - Overall CPU percentage with color indicators
- **RAM usage** - Used/Total memory in MB and percentage
- **Top Apps** - Top 3 apps by CPU/RAM consumption

The app runs as a **foreground service** with a **floating overlay window** that shows metrics in real-time.

### Key Features to Test
1. ✅ **Overlay Display** - Floating window showing real-time metrics
2. ✅ **CPU Monitoring** - Accurate CPU usage (0-100%)
3. ✅ **RAM Monitoring** - Current memory consumption
4. ✅ **Top Apps Tracking** - Top 3 resource-hungry apps
5. ✅ **Adaptive Performance** - Adjusts update interval based on load
6. ✅ **Position Persistence** - Saves overlay position (mobile only)

---

## 🔧 Setup & Prerequisites

### Required Tools

1. **Android Debug Bridge (ADB)**
   ```bash
   # Verify ADB installed
   adb version
   # Should show: Android Debug Bridge version 1.0.41+
   ```

2. **Android Device or Emulator**
   - Android 8.0+ (API 26+)
   - Developer Options enabled
   - USB Debugging enabled

3. **Terminal/Command Prompt**
   - Windows: PowerShell or CMD
   - Mac/Linux: Terminal

### Device Setup

**Enable Developer Options:**
1. Go to Settings → About Phone
2. Tap "Build Number" 7 times
3. Developer Options now available

**Enable USB Debugging:**
1. Settings → Developer Options
2. Enable "USB Debugging"
3. Connect device via USB
4. Accept debugging prompt on device

**Verify Connection:**
```bash
adb devices
# Should show your device:
# List of devices attached
# ABC123456789    device
```

---

## 📱 Installation

### Install Debug APK

**Method 1: Via ADB**
```bash
# Navigate to APK location
cd /path/to/apk

# Install
adb install app-debug.apk

# Or reinstall (if already installed)
adb install -r app-debug.apk
```

**Method 2: Via File Manager**
1. Transfer APK to device
2. Open file manager
3. Tap APK file
4. Allow "Install from unknown sources" if prompted
5. Tap "Install"

**Verify Installation:**
```bash
adb shell pm list packages | grep sysmetrics
# Should show: package:com.sysmetrics.app
```

---

## 🧪 Core Testing Scenarios

### Test 1: First Launch & Permissions

**Objective:** Verify app launches and permission flow works

**Steps:**
1. Launch "SysMetrics" from app drawer
2. Observe main screen
3. Tap "Start Monitor" button
4. **Expected:** Overlay permission dialog appears
5. Tap "Grant Permission"
6. **Expected:** Settings screen opens
7. Find "SysMetrics" and enable permission
8. Return to app
9. **Expected:** Foreground notification appears
10. **Expected:** Overlay window appears within 2 seconds

**Pass Criteria:**
- ✅ App launches without crash
- ✅ Permission dialog shown
- ✅ Permission granted successfully
- ✅ Overlay displays after permission grant
- ✅ Foreground notification visible

**Fail If:**
- ❌ App crashes on launch
- ❌ Permission dialog doesn't appear
- ❌ Overlay doesn't show after permission grant
- ❌ No foreground notification

---

### Test 2: CPU Monitoring Accuracy

**Objective:** Verify CPU shows realistic non-zero values

**Steps:**
1. Start overlay monitor
2. Wait 2-3 seconds (baseline initialization)
3. Observe CPU value
4. Open Chrome/heavy app
5. Browse or scroll
6. Observe CPU value changes

**Check LogCat:**
```bash
adb logcat -s METRICS_BASELINE:D METRICS_CPU:D
```

**Expected Output:**
```
METRICS_BASELINE: ✅ Baseline initialized
METRICS_CPU: 📈 CPU: totalΔ=645 → 48.2%
```

**Pass Criteria:**
- ✅ CPU shows 5-95% range (not 0%, not 100%)
- ✅ CPU updates every ~500ms
- ✅ CPU increases when opening heavy apps
- ✅ Color changes: Green (<50%), Yellow (50-80%), Red (>80%)
- ✅ Logs show "Using NATIVE JNI" or "using Kotlin"

**Fail If:**
- ❌ CPU stuck at 0%
- ❌ CPU stuck at 100%
- ❌ CPU doesn't update
- ❌ CPU shows unrealistic values (>100%)
- ❌ Logs show errors

---

### Test 3: RAM Monitoring

**Objective:** Verify RAM shows accurate memory usage

**Steps:**
1. Start overlay monitor
2. Note initial RAM value (e.g., "1024/2048 MB (50%)")
3. Open multiple heavy apps (Chrome, YouTube, Camera)
4. Observe RAM increase
5. Close apps
6. Observe RAM decrease

**Pass Criteria:**
- ✅ RAM shows Used/Total format
- ✅ RAM percentage matches calculation
- ✅ RAM increases when opening apps
- ✅ Values are realistic for device
- ✅ Updates in sync with CPU

**Fail If:**
- ❌ RAM shows 0 MB or negative values
- ❌ RAM > Total RAM
- ❌ RAM doesn't update
- ❌ Format incorrect

---

### Test 4: Top Apps Tracking

**Objective:** Verify top 3 apps displayed correctly

**Steps:**
1. Start overlay monitor
2. Observe "Top Apps" section
3. Open heavy app (Chrome, YouTube)
4. Wait 2-3 seconds
5. **Expected:** App appears in top 3

**Check LogCat:**
```bash
adb logcat -s PROC_TOP:D
```

**Expected Output:**
```
PROC_TOP: 🏆 #1: YouTube - CPU=23.4%, RAM=245MB
PROC_TOP: 🏆 #2: Chrome - CPU=12.1%, RAM=189MB
PROC_TOP: 🏆 #3: com.android.systemui - CPU=5.2%, RAM=95MB
```

**Pass Criteria:**
- ✅ 3 apps shown (or less if <3 active)
- ✅ App names truncated to 12 chars
- ✅ CPU and RAM shown per app
- ✅ Apps sorted by combined score
- ✅ SysMetrics NOT in top apps (self-excluded)
- ✅ Values update every 2-3 seconds

**Fail If:**
- ❌ No apps shown
- ❌ SysMetrics appears in its own list
- ❌ Values don't update
- ❌ Sorting incorrect
- ❌ App names overflow/clip

---

### Test 5: Overlay Position & Dragging

**Mobile Devices Only**

**Objective:** Verify overlay can be dragged and position persists

**Steps:**
1. Start overlay monitor
2. Tap and hold overlay window
3. Drag to different position
4. Release
5. **Expected:** Overlay stays at new position
6. Stop monitor
7. Restart monitor
8. **Expected:** Overlay appears at saved position

**Check LogCat:**
```bash
adb logcat -s OVERLAY_SERVICE:D
```

**Expected Output:**
```
OVERLAY_SERVICE: Overlay position changed: (150, 300)
OVERLAY_SERVICE: ✅ Position saved to preferences
```

**Pass Criteria:**
- ✅ Overlay responds to touch
- ✅ Overlay follows finger during drag
- ✅ Overlay snaps to finger release position
- ✅ Position saved (check logs)
- ✅ Position restored after restart

**Fail If:**
- ❌ Overlay doesn't respond to touch
- ❌ Overlay doesn't move
- ❌ Position not saved
- ❌ Overlay resets to default position after restart

**Android TV:** Dragging should be **disabled** (fixed position only)

---

### Test 6: Lifecycle & State Management

**Objective:** Verify overlay survives lifecycle events

**Scenario A: Screen Rotation** (Mobile only)
1. Start overlay monitor
2. Rotate device
3. **Expected:** Overlay stays visible
4. **Expected:** Metrics continue updating

**Scenario B: App Minimized**
1. Start overlay monitor
2. Press Home button
3. Open other apps
4. **Expected:** Overlay stays on top
5. **Expected:** Metrics continue updating

**Scenario C: Kill Main Activity**
1. Start overlay monitor
2. Press Home
3. Kill app from Recent Apps
4. **Expected:** Overlay stays visible (service independent)
5. **Expected:** Metrics continue updating

**Scenario D: Screen Lock**
1. Start overlay monitor
2. Lock screen
3. Unlock screen
4. **Expected:** Overlay still visible
5. **Expected:** Metrics resume updating

**Pass Criteria:**
- ✅ Overlay survives all scenarios
- ✅ Metrics continue updating
- ✅ No crashes
- ✅ No memory leaks (check LeakCanary)

**Fail If:**
- ❌ Overlay disappears
- ❌ App crashes
- ❌ Metrics stop updating
- ❌ LeakCanary shows leaks

---

## 📊 LogCat Monitoring

### Essential Commands

**Monitor All SysMetrics Logs:**
```bash
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_"
```

**Monitor Overlay Display (What's on screen):**
```bash
adb logcat -s OVERLAY_DISPLAY:D
```

**Monitor CPU Calculation:**
```bash
adb logcat -s METRICS_CPU:D METRICS_BASELINE:D
```

**Monitor Top Apps:**
```bash
adb logcat -s PROC_TOP:D
```

**Monitor Service Lifecycle:**
```bash
adb logcat -s OVERLAY_SERVICE:D
```

**Find Errors:**
```bash
adb logcat *:E
```

**Save Logs to File:**
```bash
adb logcat > sysmetrics_test_logs.txt
```

**Clear LogCat:**
```bash
adb logcat -c
```

### Log Tag Reference

| Tag | Purpose | Example |
|-----|---------|---------|
| `OVERLAY_SERVICE` | Service lifecycle | "✅ MinimalistOverlayService created" |
| `OVERLAY_UPDATE` | Update cycles | "✅ Update cycle completed in 23ms" |
| `OVERLAY_DISPLAY` | Screen output | "📺 CPU on SCREEN: 'CPU: 45%'" |
| `METRICS_CPU` | CPU calculation | "📈 CPU: totalΔ=645 → 48.2%" |
| `METRICS_BASELINE` | Baseline init | "✅ Baseline initialized" |
| `PROC_TOP` | Top apps | "🏆 #1: YouTube - CPU=23.4%" |
| `METRICS_ERROR` | Errors | "❌ Failed to read /proc/stat" |

### Interpreting Logs

**Healthy Startup Sequence:**
```
OVERLAY_SERVICE: ✅ MinimalistOverlayService created
OVERLAY_SERVICE: 📦 Collectors injected via Hilt
METRICS_BASELINE: 🔧 Initializing CPU baseline...
METRICS_BASELINE: 🚀 Using NATIVE JNI for CPU
METRICS_BASELINE: ✅ Baseline initialized
OVERLAY_SERVICE: ✅ Overlay created successfully
OVERLAY_UPDATE: ✅ Update cycle completed in 15ms
```

**Problem Indicators:**
```
METRICS_ERROR: ❌ Failed to read /proc/stat
OVERLAY_SERVICE: ⚠️ Baseline not initialized
METRICS_CPU: ⚠️ CPU stuck at 0%
```

---

## 🔍 Common Issues & Solutions

### Issue 1: CPU Shows 0%

**Symptoms:**
- CPU stuck at 0%
- Doesn't change with load

**Diagnosis:**
```bash
adb logcat -s METRICS_BASELINE:D METRICS_CPU:D
```

**Look for:**
- "Using NATIVE JNI" or "using Kotlin"
- "✅ Baseline initialized"

**Causes:**
1. Baseline not initialized (wait 2 seconds)
2. /proc/stat read failure (Android 10+ restriction)
3. Native library not loaded

**Solution:**
1. Wait 2-3 seconds after start
2. Check logs for "Baseline initialized"
3. Verify "Using NATIVE JNI" (bypasses restrictions)
4. If stuck, report bug with logs

---

### Issue 2: Overlay Not Showing

**Symptoms:**
- Notification appears but no overlay
- "Start Monitor" enabled but nothing visible

**Diagnosis:**
```bash
adb logcat -s OVERLAY_SERVICE:D
```

**Look for:**
- "Overlay created successfully"
- WindowManager errors

**Causes:**
1. Overlay permission not granted
2. WindowManager error
3. Overlay off-screen

**Solution:**
1. Verify permission:
   ```bash
   adb shell appops get com.sysmetrics.app SYSTEM_ALERT_WINDOW
   ```
   Should show: "Mode: allow"
   
2. Grant permission manually:
   Settings → Apps → SysMetrics → Permissions → Display over other apps

3. Check logs for WindowManager errors

---

### Issue 3: App Crash on TV

**Symptoms:**
- App crashes when hovering on Android TV
- Exception mentioning ACTION_HOVER_EXIT

**Diagnosis:**
```bash
adb logcat *:E
```

**Cause:**
- TV remote hover events triggering crash

**Expected Behavior:**
- Exception handler catches and logs hover events
- App doesn't crash

**Verify:**
```
OVERLAY_SERVICE: Uncaught exception: ACTION_HOVER_EXIT
```
Should be logged but **not crash**

---

### Issue 4: Memory Leak

**Symptoms:**
- LeakCanary notification shows leak
- RAM usage growing continuously

**Diagnosis:**
- Check LeakCanary notification
- Tap notification to see leak trace

**Common Leaks:**
- Service not stopped properly
- Coroutine scope not cancelled
- WindowManager view not removed

**Verify Fix:**
1. Run overlay for 10 minutes
2. Stop overlay
3. Check LeakCanary
4. Should show: "No leaks detected"

---

## ⚡ Performance Testing

### Memory Usage Test

**Objective:** Verify app uses <50MB RAM

**Steps:**
1. Start overlay monitor
2. Let run for 5 minutes
3. Check memory usage

**Method 1: Developer Options**
```
Settings → Developer Options → Running Services
Find "SysMetrics" → Note memory usage
```

**Method 2: ADB**
```bash
adb shell dumpsys meminfo com.sysmetrics.app | grep TOTAL
```

**Pass Criteria:**
- ✅ Total RAM <50MB
- ✅ RAM stable over time (no growth)

---

### CPU Overhead Test

**Objective:** Verify app CPU overhead <5%

**Steps:**
1. Start overlay monitor
2. Let device idle
3. Observe SysMetrics CPU in top apps
4. Should be very low or not in top 3

**Verify in Logs:**
```bash
adb logcat -s PROC_TOP:D
```

SysMetrics should **NOT** appear in its own top apps list

**Pass Criteria:**
- ✅ SysMetrics CPU <5% during idle
- ✅ SysMetrics excluded from top apps

---

### Battery Drain Test

**Objective:** Verify battery drain <2% per hour

**Steps:**
1. Charge device to 100%
2. Disconnect charger
3. Start overlay monitor
4. Let run for 1 hour
5. Note battery percentage

**Pass Criteria:**
- ✅ Battery drop <2% in 1 hour
- ✅ No excessive drain warnings

**Note:** This is a long test, can be optional

---

## 🐛 Bug Reporting

### When to Report a Bug

Report if you observe:
- ❌ **Crash** - App stops unexpectedly
- ❌ **Freeze** - App becomes unresponsive (ANR)
- ❌ **Incorrect Data** - CPU/RAM values wrong
- ❌ **Memory Leak** - LeakCanary reports leak
- ❌ **UI Issue** - Text overflow, misalignment
- ❌ **Missing Feature** - Expected functionality not working

### How to Report

**Use BUG_REPORT_TEMPLATE.md:**
1. Copy template
2. Fill all sections
3. Attach logs and screenshots
4. Submit to issue tracker

**Essential Information:**
1. **Device & Android version**
2. **Reproduction steps** (detailed!)
3. **Expected vs Actual behavior**
4. **LogCat output** (use commands above)
5. **Screenshots** if UI issue

**Collect Logs:**
```bash
# Reproduce bug, then:
adb logcat -d > bug_$(date +%Y%m%d_%H%M%S).txt
```

---

## ✅ Quick Test Checklist

Before reporting "PASS":

- [ ] App installs successfully
- [ ] Permissions granted
- [ ] Overlay displays within 2 seconds
- [ ] CPU shows 5-95% range (not 0%)
- [ ] RAM shows realistic values
- [ ] Top 3 apps displayed correctly
- [ ] Overlay draggable on mobile
- [ ] Position persists after restart (mobile)
- [ ] Survives screen rotation (mobile)
- [ ] Survives app minimize
- [ ] No crashes in 10-minute session
- [ ] LeakCanary shows no leaks
- [ ] Memory usage <50MB
- [ ] All logs show healthy status

---

## 📞 Support

**Questions:** Contact Android Tech Lead  
**Documentation:** See README.md, DEVELOPMENT.md  
**Logging Guide:** See LOGGING_GUIDE.md  

---

*Happy Testing! 🚀*
