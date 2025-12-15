# Bug Report - SysMetrics Pro

**Report Date:** _______________  
**Reporter:** _______________  
**Bug ID:** _______________  

---

## 🔴 Bug Information

### Title
_Short descriptive title (max 80 chars)_

**Example:** "CPU shows 0% on Android 13 devices"

---

### Severity
Select one:

- [ ] 🔴 **Critical** - App crashes, data loss, core functionality broken
- [ ] 🟡 **High** - Major feature not working, significant UX issue
- [ ] 🟢 **Medium** - Minor feature broken, workaround available
- [ ] ⚪ **Low** - Cosmetic issue, typo, minor UI glitch

---

### Priority
Select one:

- [ ] 🔴 **Urgent** - Must fix before release
- [ ] 🟡 **High** - Should fix in this release
- [ ] 🟢 **Medium** - Can fix in next release
- [ ] ⚪ **Low** - Nice to have

---

### Component
Select one or more:

- [ ] Overlay Display
- [ ] CPU Monitoring
- [ ] RAM Monitoring
- [ ] Top Apps
- [ ] Settings
- [ ] Permissions
- [ ] Service/Notification
- [ ] UI/Layout
- [ ] Performance
- [ ] Other: _______________

---

## 📱 Environment

### Device Information
```
Device Model: _____________________
Manufacturer: _____________________
Android Version: __________________
API Level: ________________________
Screen Size: ______________________
Screen DPI: _______________________
RAM: ______________________________
```

### App Information
```
App Version: 1.5.0
Version Code: 1
Build Type: Debug
Build Date: 2025-12-15
```

### Additional Context
```
LeakCanary Active: [ ] Yes [ ] No
Native JNI: [ ] Enabled [ ] Disabled
Overlay Permission: [ ] Granted [ ] Denied
Other Apps Running: _______________
```

---

## 🔄 Reproduction Steps

**Prerequisites:**
_What needs to be set up before reproducing?_
1. 
2. 

**Steps to Reproduce:**
1. 
2. 
3. 
4. 

**Frequency:**
- [ ] Always (100%)
- [ ] Often (>75%)
- [ ] Sometimes (25-75%)
- [ ] Rare (<25%)
- [ ] Once

---

## ❌ Expected Behavior

_What should happen?_




---

## ⚠️ Actual Behavior

_What actually happens?_




---

## 📸 Evidence

### Screenshots
_Attach screenshots if applicable_

- [ ] Screenshot 1: _______________
- [ ] Screenshot 2: _______________
- [ ] Screen Recording: _______________

### LogCat Output
_Provide relevant logs using these commands:_

```bash
# Get all SysMetrics logs
adb logcat | grep -E "OVERLAY_|METRICS_|PROC_" > bug_logs.txt

# Get crash logs
adb logcat *:E > error_logs.txt

# Get specific component logs
adb logcat -s OVERLAY_SERVICE:D OVERLAY_UPDATE:D METRICS_CPU:D
```

**Attach log file:** _______________

**Key log excerpts:**
```
[Paste relevant log lines here]



```

---

## 🔍 Analysis

### Root Cause (if known)
_What's causing the issue?_




### Affected Code (if known)
```
File: ______________________________
Function: ___________________________
Lines: ______________________________
```

---

## 🔧 Workaround

_Is there a temporary workaround?_

- [ ] Yes - _describe:_ _______________
- [ ] No

---

## 💡 Suggested Fix

_Your suggestion for fixing (optional)_




---

## 📋 Additional Information

### Related Issues
_Link to similar bugs or related tickets_
- 
- 

### Testing Notes
```
Tested on other devices: [ ] Yes [ ] No
If yes, list devices and results:
- 
- 
```

### Impact Assessment
```
Users Affected: [ ] All [ ] Most [ ] Some [ ] Few
Feature Affected: [ ] Core [ ] Important [ ] Nice-to-have
Business Impact: [ ] High [ ] Medium [ ] Low
```

---

## 📝 Developer Notes

_Space for developer comments_




---

## ✅ Verification

### Fix Verification Steps
_How to verify the fix:_
1. 
2. 
3. 

### Regression Testing
_Areas to test after fix:_
- [ ] 
- [ ] 
- [ ] 

---

## 📞 Contact

**Reporter Email:** _______________  
**Reporter Phone:** _______________  
**Available for follow-up:** [ ] Yes [ ] No  

---

## 🏷️ Status Tracking

- [ ] **New** - Bug reported, not yet reviewed
- [ ] **Confirmed** - Reproduced by developer
- [ ] **In Progress** - Developer working on fix
- [ ] **Fixed** - Fix implemented
- [ ] **Verified** - Fix tested by QA
- [ ] **Closed** - Confirmed working

**Assigned to:** _______________  
**Target Release:** _______________  
**Fix Committed:** _______________  
**Verified by:** _______________  
**Closed Date:** _______________  

---

## 📋 Quick Reference - Common Issues

### Issue: CPU shows 0%
**Check:** 
```bash
adb logcat -s METRICS_BASELINE:D METRICS_CPU:D
```
**Look for:** "Using NATIVE JNI" or "using Kotlin"
**Expected:** Non-zero CPU values within 2 seconds

### Issue: Overlay not showing
**Check:** 
```bash
adb logcat -s OVERLAY_SERVICE:D
```
**Look for:** "Overlay created", "Foreground service started"
**Verify:** Overlay permission granted in Settings

### Issue: App crash on TV
**Check:** 
```bash
adb logcat *:E
```
**Look for:** ACTION_HOVER_EXIT exceptions
**Expected:** Exception handler catches hover events

### Issue: Memory leak
**Check:** LeakCanary notification
**Look for:** Leak traces in notification
**Verify:** No leaks after 10-minute session

---

*For testing checklist, see QA_TESTING_CHECKLIST.md*  
*For testing guide, see QA_TESTING_GUIDE.md*
