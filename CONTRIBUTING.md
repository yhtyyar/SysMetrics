# Contributing to SysMetrics

Thank you for your interest in contributing to SysMetrics! This guide will help you get started.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

### Reporting Bugs

1. Check [existing issues](https://github.com/yhtyyar/SysMetrics/issues) to avoid duplicates
2. Use the **Bug Report** issue template
3. Include: Android version, device model, steps to reproduce, logcat output

### Suggesting Features

1. Open a **Feature Request** issue
2. Describe the use case and expected behavior
3. Explain why it benefits power users or Android TV users

### Submitting Code

1. Fork the repository
2. Create a feature branch from `main`:
   ```bash
   git checkout -b feat/your-feature
   ```
3. Make your changes following the [code standards](#code-standards)
4. Write or update tests
5. Commit using [Conventional Commits](#commit-style)
6. Push and open a Pull Request

## Development Setup

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1.1+ |
| JDK | 17 |
| NDK | 25.2.9519653 |
| CMake | 3.22.1 |

### Build from Source

```bash
git clone https://github.com/yhtyyar/SysMetrics.git
cd SysMetrics
./gradlew assembleDebug
```

### Run Tests

```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Instrumented tests (requires emulator or device)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.notPackage=com.sysmetrics.app.benchmark
```

## Code Standards

- **Language**: Kotlin (no Java for new code)
- **Architecture**: MVVM + Clean Architecture (domain/data/ui layers)
- **DI**: Hilt with `@Inject` constructor injection
- **Async**: Coroutines + Flow (no `runBlocking` in production code)
- **Logging**: Timber with structured tags (`METRICS_CPU`, `OVERLAY_SERVICE`, etc.)
- **Tests**: JUnit 4 + MockK for unit tests, Espresso for UI tests

See [DEVELOPMENT.md](DEVELOPMENT.md) for detailed code standards and templates.

## Commit Style

This project uses [Conventional Commits](https://www.conventionalcommits.org/) with Android module scopes:

```
type(scope): imperative description

Optional body with details.
```

**Types**: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `ci`, `chore`

**Scopes**: `app`, `ui`, `service`, `data`, `native`, `network`, `overlay`, `settings`, `build`, `test`

**Examples**:
```
feat(overlay): add opacity slider control
fix(native): use PRIu64 for portable uint64_t format
refactor(data): extract data source interfaces
ci: add GitHub Actions for CI, release, nightly
docs: update contributing guidelines
```

**Rules**:
- Subject line: imperative mood, max 72 characters, no period
- Body: wrap at 72 characters, explain *what* and *why*

## Pull Request Guidelines

- Reference the related issue (e.g., `Fixes #42`)
- Keep PRs focused — one feature or fix per PR
- Ensure CI passes (lint, unit tests, instrumented tests)
- Update documentation if behavior changes
- Add tests for new functionality

## Native Code (C++/JNI)

If modifying native code in `app/src/main/cpp/`:

- Use `<inttypes.h>` macros (`PRIu64`, `SCNu64`) for portable formatting
- Test on both 32-bit (`armeabi-v7a`) and 64-bit (`arm64-v8a`) ABIs
- Avoid heap allocations in hot paths — use stack buffers
- Always null-check JNI references

## Questions?

Open a [Discussion](https://github.com/yhtyyar/SysMetrics/discussions) or reach out via Issues.
