# QA Testing Checklist - SysMetrics Pro v1.5.0

**Build:** Debug  
**Date:** December 15, 2025  
**Tester:** _________________  
**Device:** _________________  
**Android Version:** _________  

---

## 📋 Pre-Testing Setup

- [ ] Debug APK installed successfully
- [ ] LeakCanary notification visible (debug build indicator)
- [ ] adb connected for logcat monitoring
- [ ] Device has sufficient battery (>50%)
- [ ] No other monitoring apps running

---

## 🚀 Installation & Permissions

### Installation
- [ ] APK installs without errors
- [ ] App icon visible in launcher
- [ ] App opens successfully
- [ ] No crash on first launch

### Permissions Flow
- [ ] Overlay permission dialog appears when starting monitor
- [ ] "Grant Permission" button works
- [ ] Permission granted successfully
- [ ] App handles permission denial gracefully
- [ ] Post-Notifications permission requested (Android 13+)
- [ ] Can start monitor after granting permissions

**Priority:** 🔴 Critical

---

## 📊 Core Functionality - Overlay Display

### Overlay Activation
- [ ] "Start Monitor" button enables successfully
- [ ] Foreground service notification appears
- [ ] Overlay window becomes visible
- [ ] Overlay displays within 2 seconds

### Metrics Display - CPU
- [ ] CPU percentage shows (0-100%)
- [ ] CPU value updates every ~500ms
- [ ] CPU shows realistic values (not 0%, not stuck)
- [ ] CPU color changes based on load:
  - [ ] Green: <50%
  - [ ] Yellow: 50-80%
  - [ ] Red: >80%

### Metrics Display - RAM
- [ ] RAM shows Used/Total format (e.g., "1024/2048 MB")
- [ ] RAM percentage shows (e.g., "50%")
- [ ] RAM values are realistic
- [ ] RAM updates in sync with CPU

### Metrics Display - Top Apps
- [ ] Top 3 apps section visible
- [ ] App names displayed (max 12 chars)
- [ ] CPU % shown per app
- [ ] RAM (MB) shown per app
- [ ] Apps sorted correctly by combined score
- [ ] Self app (SysMetrics) excluded from top apps

**Priority:** 🔴 Critical

---

## 🎨 UI/UX - Overlay Appearance

### Visual Design
- [ ] Overlay has dark semi-transparent background
- [ ] Text is readable (white on dark)
- [ ] No text overflow or clipping
- [ ] Proper spacing between elements
- [ ] Margins appropriate for device type (TV: 48dp, Mobile: 16dp)

### Overlay Position
- [ ] Overlay positioned correctly on first launch
- [ ] Overlay stays within screen bounds
- [ ] On mobile: draggable (tap and drag)
- [ ] On TV: fixed position (no drag)
- [ ] Position persists after app restart (mobile)

### Adaptive UI
- [ ] On Android TV: larger margins applied
- [ ] On mobile phones: compact layout
- [ ] On tablets: appropriate scaling

**Priority:** 🟡 High

---

## ⚡ Performance & Stability

### Resource Usage
- [ ] App uses <50MB RAM (check in Settings → Developer Options)
- [ ] CPU overhead <5% when idle
- [ ] No ANR (Application Not Responding) dialogs
- [ ] No frame drops in overlay updates

### Battery Impact
- [ ] Foreground service notification persistent
- [ ] Battery drain <2% per hour (test for 30 min)
- [ ] No significant battery drain in background

### Adaptive Performance
- [ ] Update interval adjusts under high load
- [ ] Slow update warning in logs (check logcat)
- [ ] Performance returns to normal after load decrease

### Memory Leaks
- [ ] LeakCanary shows no leaks during 10-minute session
- [ ] No memory leak warnings after stopping overlay
- [ ] App memory stable over time (no continuous growth)

**Priority:** 🔴 Critical

---

## 🔄 Lifecycle & State Management

### App Lifecycle
- [ ] Overlay survives screen rotation (if mobile)
- [ ] Overlay continues when app minimized
- [ ] Overlay continues when screen locked
- [ ] Overlay stops when "Stop Monitor" pressed
- [ ] Service notification dismissed when stopped

### Process Management
- [ ] Overlay survives after killing main activity
- [ ] Service restarts if killed by system (START_STICKY)
- [ ] Baseline re-initializes correctly after restart

### Edge Cases
- [ ] No crash when rapidly starting/stopping
- [ ] Handles low memory gracefully
- [ ] Handles permission revocation while running
- [ ] Works correctly after phone restart (if autostart)

**Priority:** 🟡 High

---

## 🔧 Settings & Configuration

### Settings Screen (If Implemented)
- [ ] Settings accessible from main screen
- [ ] Update interval setting works
- [ ] Top apps count setting works
- [ ] Sort by setting changes top apps list
- [ ] Settings persist after app restart

### SharedPreferences
- [ ] Overlay position saved (mobile)
- [ ] Settings loaded correctly on app start
- [ ] No crashes when preferences corrupted

**Priority:** 🟢 Medium

---

## 📱 Device Compatibility

### Android Versions
- [ ] Android 8.0 (API 26) - Min version
- [ ] Android 9.0 (API 28)
- [ ] Android 10.0 (API 29)
- [ ] Android 11.0 (API 30)
- [ ] Android 12.0 (API 31)
- [ ] Android 13.0 (API 33)
- [ ] Android 14.0 (API 34) - Target version

### Form Factors
- [ ] Mobile phone (5-6.5" screen)
- [ ] Tablet (7-11" screen)
- [ ] Android TV (TV layout)

### Special Cases
- [ ] Foldable devices (if available)
- [ ] Different DPI screens (mdpi, hdpi, xhdpi, xxhdpi)

**Priority:** 🟢 Medium

---

## 🐛 Error Handling

### Known Android 10+ Issue
- [ ] CPU shows non-zero values on Android 10+
- [ ] Native JNI fallback working (check logs: "Using NATIVE JNI")
- [ ] If native fails, Kotlin fallback used (check logs: "using Kotlin")
- [ ] Error messages logged properly

### Crash Recovery
- [ ] App doesn't crash on TV hover events
- [ ] Exception handler catches and logs crashes
- [ ] App recovers from caught exceptions

### Edge Cases
- [ ] Handles missing /proc/stat gracefully
- [ ] Handles /proc read errors
- [ ] Handles WindowManager exceptions
- [ ] No crash when permissions revoked mid-operation

**Priority:** 🔴 Critical

---

## 📝 Logging & Debugging

### LogCat Verification
Check these tags in logcat:

- [ ] `OVERLAY_SERVICE` - Service lifecycle logs present
- [ ] `OVERLAY_UPDATE` - Update cycles logged
- [ ] `OVERLAY_DISPLAY` - Screen output logged
- [ ] `METRICS_CPU` - CPU calculation details present
- [ ] `METRICS_BASELINE` - Baseline initialization logged
- [ ] `PROC_TOP` - Top apps collection logged

### Debug Commands
Test these adb commands work:

```bash
# Monitor overlay display
adb logcat -s OVERLAY_DISPLAY:D

# Monitor CPU calculation
adb logcat -s METRICS_CPU:D METRICS_BASELINE:D

# Monitor all SysMetrics
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_"
```

**Priority:** 🟡 High

---

## 🔍 Regression Testing

### Previous Bug Fixes
- [ ] CPU not stuck at 0% (Android 10+ fix)
- [ ] Baseline initialization working (1-second delay)
- [ ] No ACTION_HOVER_EXIT crashes on TV
- [ ] Top apps calculation accurate
- [ ] Memory leak fixes validated by LeakCanary

**Priority:** 🟡 High

---

## 📊 Test Results Summary

### Critical Issues (🔴)
_List any critical failures:_
- 
- 

### High Priority Issues (🟡)
_List high priority issues:_
- 
- 

### Medium Priority Issues (🟢)
_List medium priority issues:_
- 
- 

### Overall Status
- [ ] ✅ **PASS** - Ready for release
- [ ] ⚠️ **PASS with Issues** - Minor issues, can release
- [ ] ❌ **FAIL** - Critical issues, cannot release

---

## 📝 Notes & Observations

_Additional notes, edge cases, or observations:_





---

## 📞 Sign-off

**Tested by:** _________________  
**Date:** _________________  
**Signature:** _________________  

**Reviewed by:** _________________  
**Date:** _________________  

---

*For bug reports, use BUG_REPORT_TEMPLATE.md*  
*For testing guide, see QA_TESTING_GUIDE.md*
