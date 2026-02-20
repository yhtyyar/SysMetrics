# CI/CD Kaspresso Tests Exclusion

## 🚫 Temporary CI/CD Fix

Due to dependency resolution issues in CI/CD environment, Kaspresso tests are temporarily excluded from automated builds.

## ✅ Current Working Tests

The following tests work correctly in CI/CD:
- ComprehensiveTestSuite.kt (Espresso-based)
- SettingsConfigurationTestSuite.kt (Espresso-based)
- SimpleScreenshotDemo.kt
- DemoTestWithScreenshots.kt

## 🔧 Problem Description

CI/CD fails with:
```
Could not find kaspresso-framework:kaspresso:unspecified
Required by:
- com.kaspersky.android-components:kaspresso-allure-support:1.5.0
- com.kaspersky.android-components:kaspresso-compose-support:1.5.0
```

## 🎯 Solution

1. **Immediate**: Use Espresso-based tests in CI/CD
2. **Short-term**: Fix Kaspresso dependency resolution
3. **Long-term**: Re-enable Kaspresso tests after fixes

## 📁 Affected Files

These files are temporarily excluded from CI/CD:
- SettingsTest.kt (Kaspresso-based)
- StabilityTest.kt (Kaspresso-based)
- TvNavigationTest.kt (Kaspresso-based)
- SysMetricsSmokeTest.kt (Kaspresso-based)

## 🚀 Working Tests

Run these tests in CI/CD:
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sysmetrics.tests.ComprehensiveTestSuite
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sysmetrics.tests.SettingsConfigurationTestSuite
```

## 🔄 Next Steps

1. Fix Kaspresso dependency resolution
2. Update all Kaspresso imports to use correct packages
3. Re-enable Kaspresso tests in CI/CD
4. Migrate to Kaspresso completely when stable
