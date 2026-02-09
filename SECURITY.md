# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| 2.7.x   | :white_check_mark: |
| < 2.7   | :x:                |

## Reporting a Vulnerability

**Do NOT open a public issue for security vulnerabilities.**

Instead, please report security issues by emailing the maintainer directly or using [GitHub Security Advisories](https://github.com/yhtyyar/SysMetrics/security/advisories/new).

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Affected versions
- Potential impact

### Response Timeline

- **Acknowledgment**: within 48 hours
- **Assessment**: within 7 days
- **Fix release**: within 30 days for critical issues

## Security Considerations

### Permissions

SysMetrics requests only the minimum permissions required:

| Permission | Purpose | Risk Level |
|------------|---------|------------|
| `SYSTEM_ALERT_WINDOW` | Display floating overlay | Medium — user must explicitly grant |
| `FOREGROUND_SERVICE` | Keep monitoring service alive | Low |
| `POST_NOTIFICATIONS` | Service notification (Android 13+) | Low |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on boot (optional) | Low |

### Data Privacy

- All metrics are collected and stored **locally only**
- No data is transmitted to external servers
- No analytics or tracking SDKs are included
- Export functionality requires explicit user action
- Room database auto-cleans data older than 24 hours

### Native Code (JNI)

- C++ code reads only from `/proc` and `/sys` virtual filesystems
- No network operations in native layer
- Stack-allocated buffers with bounds checking
- No dynamic memory allocation in hot paths

### Signing

- Release signing keys are **never** committed to the repository
- CI/CD uses GitHub Secrets for signing credentials
- Debug builds use the Android default debug keystore
