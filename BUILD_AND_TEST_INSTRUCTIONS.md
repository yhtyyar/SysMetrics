# Build & Test Instructions - For Tech Lead

**Project:** SysMetrics Pro v1.5.1  
**Date:** December 15, 2025  
**Status:** Ready for Build & QA  

---

## 🎯 What Was Done

### Code Refactoring ✅
- **Fixed:** TODO in `MinimalistOverlayService.kt` - overlay position now saves to preferences
- **Improved:** Proper coroutine usage with `lifecycleScope`
- **Added:** Injection of `PreferencesDataSource` for position persistence

### QA Documentation Created ✅
1. **QA_TESTING_CHECKLIST.md** - 99 test points for systematic testing
2. **BUG_REPORT_TEMPLATE.md** - Standardized bug reporting format
3. **QA_TESTING_GUIDE.md** - 500+ line comprehensive testing guide
4. **REFACTORING_AND_QA_SUMMARY.md** - Complete summary of changes

---

## 🔨 Build Instructions

### Build Debug APK

**Command:**
```bash
cd /home/tester/CascadeProjects/SysMetrics

# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug
```

**Expected Output:**
```
BUILD SUCCESSFUL in Xs
```

**APK Location:**
```
app/build/outputs/apk/debug/app-debug.apk
```

### Verify Build

**Check APK exists:**
```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

**Should show:**
```
-rw-r--r-- 1 user user 15M Dec 15 17:00 app-debug.apk
```

---

## 📱 Installation

### Option 1: ADB Install (Recommended)

**Prerequisites:**
- Android device connected via USB
- USB Debugging enabled
- ADB installed

**Steps:**
```bash
# Verify device connected
adb devices
# Should show: ABC123456789    device

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or reinstall (if already installed)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Launch App:**
```bash
adb shell am start -n com.sysmetrics.app/.ui.MainActivity
```

### Option 2: Manual Install

1. Copy APK to device:
   ```bash
   adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
   ```

2. On device:
   - Open File Manager
   - Navigate to Downloads
   - Tap `app-debug.apk`
   - Allow "Unknown sources" if prompted
   - Tap Install

---

## 🧪 Testing

### Quick Smoke Test (5 minutes)

**Goal:** Verify basic functionality works

```bash
# Start logcat monitoring
adb logcat -c
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_"

# On device:
1. Open SysMetrics
2. Tap "Start Monitor"
3. Grant overlay permission
4. Verify overlay appears
5. Wait 3 seconds
6. Check CPU shows 10-90% (not 0%)
7. Check RAM shows realistic values
8. Drag overlay (mobile only)
9. Stop monitor
10. Start monitor again
11. Verify position restored (mobile)

# Check logs for:
✅ "Baseline initialized"
✅ "Using NATIVE JNI"
✅ "Overlay created successfully"
✅ "Position saved to preferences" (mobile)
❌ No errors
```

**Pass Criteria:**
- ✅ All steps complete without crash
- ✅ CPU shows non-zero values
- ✅ Logs show healthy status
- ✅ Position saves on mobile

### Full Testing

**Use QA Documentation:**
```bash
# Review testing checklist
cat QA_TESTING_CHECKLIST.md

# Review testing guide
cat QA_TESTING_GUIDE.md

# Give to QA team with these files:
- QA_TESTING_CHECKLIST.md
- QA_TESTING_GUIDE.md
- BUG_REPORT_TEMPLATE.md
- REFACTORING_AND_QA_SUMMARY.md
```

---

## 🐛 If Something Doesn't Work

### Scenario 1: CPU Shows 0%

**Check Logs:**
```bash
adb logcat -s METRICS_BASELINE:D METRICS_CPU:D
```

**Look For:**
- "✅ Baseline initialized"
- "Using NATIVE JNI" or "using Kotlin"

**If Still 0%:**
1. Wait full 3 seconds after start
2. Check Android version (10+ needs native)
3. Check logs for errors
4. Report bug using template

### Scenario 2: Overlay Not Showing

**Check Logs:**
```bash
adb logcat -s OVERLAY_SERVICE:D
```

**Look For:**
- "Overlay created successfully"

**Verify Permission:**
```bash
adb shell appops get com.sysmetrics.app SYSTEM_ALERT_WINDOW
```

**Should Show:**
```
Mode: allow
```

**If Denied:**
```bash
# Grant manually
Settings → Apps → SysMetrics → Permissions → Display over other apps → Allow
```

### Scenario 3: Position Not Saving (Mobile)

**This is the NEW feature - just fixed!**

**Check Logs:**
```bash
adb logcat -s OVERLAY_SERVICE:D
```

**Expected When Dragging:**
```
OVERLAY_SERVICE: Overlay position changed: (150, 300)
OVERLAY_SERVICE: ✅ Position saved to preferences
```

**Test Steps:**
1. Start overlay
2. Drag to new position
3. Check logs for "Position saved"
4. Stop overlay
5. Start overlay again
6. Verify overlay appears at saved position

**If Not Working:**
- This is a NEW bug - report it!
- Use BUG_REPORT_TEMPLATE.md
- Include logs with drag event

### Scenario 4: Build Failed

**Check Gradle Output:**
```bash
./gradlew assembleDebug --stacktrace
```

**Common Issues:**
1. **NDK not installed**
   - Install NDK 25.2.9519653 in Android Studio
   - SDK Manager → SDK Tools → NDK

2. **Java version wrong**
   ```bash
   java -version
   # Should be Java 17
   ```

3. **Gradle daemon issue**
   ```bash
   ./gradlew --stop
   ./gradlew assembleDebug
   ```

---

## 📊 How to Report Bugs

### Quick Template

When you find a bug, provide:

**1. Title:**
```
"[Component] Short description"
Example: "[CPU] Shows 0% on Android 13"
```

**2. Environment:**
```
Device: Samsung Galaxy S21
Android: 13 (API 33)
Build: Debug 1.5.0
```

**3. Steps:**
```
1. Open app
2. Start monitor
3. Wait 3 seconds
4. Observe CPU = 0%
```

**4. Logs:**
```bash
adb logcat -d > bug_cpu_zero.txt
```

**5. Screenshot:**
- Attach screenshot showing issue

### Full Template

Use `BUG_REPORT_TEMPLATE.md` for detailed reporting.

---

## 🔍 LogCat Cheat Sheet

### Monitor Everything
```bash
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_"
```

### Specific Components
```bash
# Overlay service
adb logcat -s OVERLAY_SERVICE:D OVERLAY_UPDATE:D

# CPU monitoring
adb logcat -s METRICS_CPU:D METRICS_BASELINE:D

# Top apps
adb logcat -s PROC_TOP:D

# Errors only
adb logcat *:E
```

### Save Logs
```bash
# Save all logs
adb logcat -d > sysmetrics_full.txt

# Save filtered logs
adb logcat -d | grep -E "OVERLAY_|METRICS_|PROC_" > sysmetrics_filtered.txt

# Save with timestamp
adb logcat -d > sysmetrics_$(date +%Y%m%d_%H%M%S).txt
```

### Clear Logs
```bash
adb logcat -c
```

---

## ✅ Pre-Release Checklist

Before releasing to QA:

**Build:**
- [ ] Code compiled without errors
- [ ] APK generated successfully
- [ ] APK size reasonable (<20MB)

**Smoke Test:**
- [ ] App installs on device
- [ ] App launches without crash
- [ ] Overlay permission can be granted
- [ ] Overlay displays
- [ ] CPU shows non-zero values
- [ ] RAM shows realistic values
- [ ] Top apps displayed
- [ ] Position saves on mobile (NEW!)

**Documentation:**
- [ ] QA_TESTING_CHECKLIST.md created
- [ ] QA_TESTING_GUIDE.md created
- [ ] BUG_REPORT_TEMPLATE.md created
- [ ] REFACTORING_AND_QA_SUMMARY.md created

**Handoff:**
- [ ] APK provided to QA
- [ ] Documentation provided to QA
- [ ] Known issues documented
- [ ] Testing priorities communicated

---

## 📅 Testing Timeline

**Suggested QA Schedule:**

**Day 1 (4 hours):**
- Setup & Installation
- Smoke testing
- Critical features

**Day 2 (4 hours):**
- Functional testing (checklist)
- High priority features

**Day 3 (4 hours):**
- Performance testing
- Stability testing

**Day 4 (4 hours):**
- Compatibility testing
- Edge cases

**Day 5 (2 hours):**
- Bug verification
- Final sign-off

**Total:** ~18 hours of testing

---

## 🎯 Success Criteria

### Must Pass (Critical)
- ✅ No crashes
- ✅ CPU monitoring works (not 0%)
- ✅ RAM monitoring works
- ✅ Overlay displays
- ✅ Permission flow works
- ✅ No memory leaks

### Should Pass (High)
- ✅ Top apps tracking works
- ✅ Adaptive performance works
- ✅ Lifecycle management works
- ✅ Position saves (mobile)

### Nice to Have (Medium)
- ✅ Performance metrics met
- ✅ All devices compatible
- ✅ UI polish complete

---

## 📞 Support & Questions

**Build Issues:**
- Check `DEVELOPMENT.md`
- Run with `--stacktrace` flag

**Testing Questions:**
- See `QA_TESTING_GUIDE.md`
- Check `LOGGING_GUIDE.md`

**Bug Reports:**
- Use `BUG_REPORT_TEMPLATE.md`
- Include logs and screenshots

**Code Questions:**
- See `DEVELOPMENT.md`
- Check inline KDoc comments

---

## 🚀 Next Steps

1. **Complete Build**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Run Smoke Test**
   - Install on test device
   - Verify basic functionality
   - Check logs

3. **Handoff to QA**
   - Provide APK
   - Provide documentation:
     - QA_TESTING_CHECKLIST.md
     - QA_TESTING_GUIDE.md
     - BUG_REPORT_TEMPLATE.md
   - Brief QA on priorities

4. **Monitor Testing**
   - Review bug reports daily
   - Prioritize critical issues
   - Fix and re-test

5. **Final Verification**
   - Verify all bugs fixed
   - Run regression tests
   - Sign off for release

---

## 📋 Document Index

**For You (Tech Lead):**
- 📄 This file - Build & test instructions
- 📝 REFACTORING_AND_QA_SUMMARY.md - What was done

**For QA Team:**
- ✅ QA_TESTING_CHECKLIST.md - Systematic checklist
- 📖 QA_TESTING_GUIDE.md - Detailed guide
- 🐛 BUG_REPORT_TEMPLATE.md - Bug template

**For Developers:**
- 📚 DEVELOPMENT.md - Dev guide
- 🔍 LOGGING_GUIDE.md - Logging reference

**For All:**
- 📖 README.md - Project overview

---

## 💡 Tips

1. **Always test on real device** - Emulator may hide issues
2. **Test on Android 10+** - CPU reading restrictions
3. **Test on TV if available** - Different input model
4. **Monitor logs continuously** - Catch issues early
5. **Test edge cases** - Rotate, low memory, etc.
6. **Use LeakCanary** - Memory leaks are bugs
7. **Document everything** - Screenshots + logs
8. **Communicate early** - Don't wait to report bugs

---

**Status:** ✅ Ready for Build & QA  
**Prepared by:** Android Tech Lead  
**Date:** December 15, 2025  

---

*Good luck with the build and testing! 🚀*
