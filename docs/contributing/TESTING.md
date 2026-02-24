# SysMetrics UI Testing Documentation

## Overview

This document describes the UI automation testing framework for SysMetrics, an Android TV application for real-time system metrics monitoring. The testing framework uses **Kaspresso** with Allure reporting support.

## Table of Contents

- [Framework Architecture](#framework-architecture)
- [Project Structure](#project-structure)
- [Test Categories](#test-categories)
- [Running Tests](#running-tests)
- [Page Objects](#page-objects)
- [Test Reports](#test-reports)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)

## Framework Architecture

### Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Kaspresso | 1.5.0 | Primary UI testing framework |
| Espresso | 3.5.1 | Android UI interactions |
| UI Automator | 2.3.0 | System-level interactions |
| JUnit | 4.13.2 | Test runner |
| Allure | 1.5.0 | Test reporting |

### Key Features

- **Page Object Pattern**: Separates UI element definitions from test logic
- **Flaky-safe Operations**: Built-in retry mechanisms for unstable tests
- **D-Pad Navigation Support**: Specialized for Android TV remote control testing
- **Automatic Screenshots**: Captured on test failures
- **Step-based Logging**: Structured test execution flow
- **Allure Integration**: Rich HTML test reports

## Project Structure

```
app/src/androidTest/
├── kotlin/com/sysmetrics/
│   ├── screens/              # Page Objects
│   │   ├── MainScreen.kt     # MainActivity page object
│   │   └── SettingsScreen.kt # SettingsActivity page object
│   ├── tests/                # Test cases
│   │   ├── SysMetricsSmokeTest.kt  # Basic smoke tests
│   │   ├── SettingsTest.kt         # Settings screen tests
│   │   ├── TvNavigationTest.kt     # D-Pad navigation tests
│   │   └── StabilityTest.kt        # Long-running stability tests
│   ├── steps/                # Reusable test steps
│   │   ├── MainScreenSteps.kt
│   │   └── SettingsScreenSteps.kt
│   └── utils/                # Utilities
│       ├── TvNavigationUtils.kt    # D-Pad operations
│       └── ScreenshotInterceptor.kt # Failure interceptors
└── resources/                # Test resources (if needed)
```

## Test Categories

### 1. Smoke Tests (`SysMetricsSmokeTest.kt`)

Basic verification of core application functionality.

| Test Case | Requirement ID | Description |
|-----------|---------------|-------------|
| `appLaunchAndBasicNavigationTest` | SMOKE-001/002/003 | App launch, overlay toggle, navigation to settings |
| `metricsUpdateTest` | SMOKE-004 | Real-time metrics update verification |

**Test Coverage:**
- Application startup
- Overlay service start/stop
- Metrics preview display
- Navigation between screens

### 2. Settings Tests (`SettingsTest.kt`)

Comprehensive testing of the Settings screen functionality.

| Test Case | Requirement ID | Description |
|-----------|---------------|-------------|
| `overlayPositionConfigurationTest` | SET-001/002 | Position radio button selection |
| `metricToggleConfigurationTest` | SET-003/004 | CPU/RAM/Time toggle switches |
| `backgroundCollectionToggleTest` | SET-005 | Background data collection toggle |
| `exportButtonsVisibilityTest` | SET-006/007 | Export CSV/JSON button verification |
| `combinedSettingsConfigurationTest` | SET-008 | Multi-setting configuration scenario |

**Test Coverage:**
- 4 overlay position options (Top-Left, Top-Right, Bottom-Left, Bottom-Right)
- 3 metric display toggles (CPU, RAM, Time)
- Data export functionality (CSV, JSON)
- Background collection settings

### 3. TV Navigation Tests (`TvNavigationTest.kt`)

Android TV specific D-Pad navigation tests. These tests are **skipped** on non-TV devices.

| Test Case | Requirement ID | Description |
|-----------|---------------|-------------|
| `dpadNavigationBetweenButtonsTest` | TV-001/002 | D-Pad navigation between UI elements |
| `dpadNavigationToSettingsTest` | TV-003 | Navigation to Settings screen using D-Pad |
| `focusManagementTest` | TV-004 | Focus animation and visibility verification |

**Prerequisites:**
- Android TV device or emulator
- `Configuration.UI_MODE_TYPE_TELEVISION`

### 4. Stability Tests (`StabilityTest.kt`)

Long-running tests for reliability verification.

| Test Case | Requirement ID | Description |
|-----------|---------------|-------------|
| `overlayToggleStabilityTest` | STAB-001 | Multiple start/stop cycles (10 iterations) |
| `metricsUpdateStabilityTest` | STAB-002 | 60-second metrics update monitoring |
| `memoryStabilityTest` | STAB-003 | RAM, temperature, network display verification |
| `applicationStateConsistencyTest` | STAB-004 | State consistency across operations |

## Running Tests

### Prerequisites

1. Android device or emulator connected
2. Minimum API level: 21 (Android 5.0)
3. Target API level: 34 (Android 14)

### Execute All Tests

```bash
./gradlew connectedAndroidTest
```

### Execute Specific Test Class

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sysmetrics.tests.SysMetricsSmokeTest
```

### Execute Specific Test Method

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sysmetrics.tests.SysMetricsSmokeTest#appLaunchAndBasicNavigationTest
```

### With Allure Reporting

```bash
# Run tests
./gradlew connectedAndroidTest

# Generate Allure report
./gradlew allureReport

# Serve report locally
./gradlew allureServe
```

### TV-Specific Testing

For Android TV testing, use an emulator with TV configuration:

```bash
# Create TV emulator
avdmanager create avd -n tv_emulator -k "system-images;android-34;android-tv;x86_64" --device "tv_1080p"

# Launch emulator
emulator -avd tv_emulator

# Run TV tests
./gradlew connectedAndroidTest
```

## Page Objects

### MainScreen

Represents the MainActivity with overlay toggle and metrics preview.

```kotlin
object MainScreen : KScreen<MainScreen>() {
    val appTitle = KTextView { withId(R.id.tv_app_title) }
    val statusText = KTextView { withId(R.id.tv_status) }
    val toggleButton = KButton { withId(R.id.btn_toggle_overlay) }
    val settingsButton = KButton { withId(R.id.btn_settings) }
    val metricsPreviewLayout = KView { withId(R.id.layout_metrics_preview) }
    val cpuPreview = KTextView { withId(R.id.tv_cpu_preview) }
    val ramPreview = KTextView { withId(R.id.tv_ram_preview) }
    val tempPreview = KTextView { withId(R.id.tv_temp_preview) }
    val networkPreview = KTextView { withId(R.id.tv_network_preview) }
}
```

### SettingsScreen

Represents the SettingsActivity with configuration options.

```kotlin
object SettingsScreen : KScreen<SettingsScreen>() {
    val positionTopLeft = KRadioButton { withId(R.id.rb_top_left) }
    val positionTopRight = KRadioButton { withId(R.id.rb_top_right) }
    val positionBottomLeft = KRadioButton { withId(R.id.rb_bottom_left) }
    val positionBottomRight = KRadioButton { withId(R.id.rb_bottom_right) }
    val switchCpu = KSwitch { withId(R.id.switch_cpu) }
    val switchRam = KSwitch { withId(R.id.switch_ram) }
    val switchTime = KSwitch { withId(R.id.switch_time) }
    val exportCsvButton = KButton { withId(R.id.btn_export_csv) }
    val exportJsonButton = KButton { withId(R.id.btn_export_json) }
    val saveButton = KButton { withId(R.id.btn_save) }
}
```

## Test Reports

### Allure Report Structure

```
allure-results/
├── *.json          # Test case results
└── attachments/    # Screenshots and logs
```

### Report Features

- **Test Cases**: Grouped by test class and requirements
- **Steps**: Hierarchical step visualization
- **Attachments**: Automatic screenshots on failure
- **History**: Test execution trends
- **Categories**: Smoke, Settings, TV, Stability

### Generating Reports

```bash
# After test execution
./gradlew allureReport

# Report location
open app/build/reports/allure-report/index.html
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: UI Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  ui-test:
    runs-on: macos-latest  # Required for emulator
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Run UI Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 29
          script: ./gradlew connectedAndroidTest
          
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        with:
          name: allure-results
          path: app/build/allure-results
```

### Firebase Test Lab Integration

```bash
# Upload to Firebase Test Lab
gcloud firebase test android run \
  --type instrumentation \
  --app app/build/outputs/apk/debug/app-debug.apk \
  --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=panther,version=33,locale=en,orientation=portrait \
  --timeout 30m
```

## Troubleshooting

### Common Issues

#### 1. Test fails with "No matching view found"

**Cause:** UI element not visible or ID mismatch

**Solution:**
```kotlin
// Use flakySafely for timing issues
flakySafely(timeout = 5000) {
    MainScreen.toggleButton.isVisible()
}
```

#### 2. TV tests skip on mobile device

**Expected behavior:** TV tests check `isTvDevice()` and skip automatically

**Verification:**
```bash
# Check UI mode in logcat
adb shell getprop ro.build.characteristics
# Should contain "tv"
```

#### 3. Overlay permission denied

**Cause:** SYSTEM_ALERT_WINDOW permission not granted

**Solution:**
```bash
# Grant permission via ADB
adb shell appops set com.sysmetrics.app DEBUG_ALLOW_MOCK_LOCATION allow
adb shell settings put secure enabled_input_methods "com.sysmetrics.app"
```

#### 4. Metrics not updating

**Cause:** Service not properly started

**Solution:**
- Verify overlay is started before checking metrics
- Use `flakySafely` with adequate timeout
- Check logcat for service errors

### Debug Commands

```bash
# View test logs
adb logcat -s SysMetrics:D

# Check service status
adb shell dumpsys activity services com.sysmetrics.app

# Capture screenshot manually
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png
```

### Performance Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Smoke Test Duration | < 30s | ~25s |
| Settings Test Duration | < 60s | ~45s |
| Flakiness Rate | < 5% | ~2% |
| Coverage | > 80% | ~85% |

## Maintenance Guidelines

### Adding New Tests

1. Create Page Object if new screen is added
2. Add reusable steps to `steps/` directory
3. Use `step()` for logical grouping
4. Add `@Requirements` annotation for traceability
5. Include cleanup in `after` block

### Updating Existing Tests

1. Check for layout ID changes
2. Update timeouts if needed
3. Run full regression before committing
4. Update this documentation

## Contact & Support

For issues related to UI tests:
- Create an issue in the repository
- Include logcat output and Allure report
- Specify device/emulator configuration

---

**Document Version:** 1.0  
**Last Updated:** 2024-02-18  
**Compatible with:** SysMetrics v2.7.0+
