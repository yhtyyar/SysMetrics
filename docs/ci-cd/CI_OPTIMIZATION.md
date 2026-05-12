# CI/CD Optimization — SysMetrics Pro

Applied 2026-05-12. Covers all three workflow files plus a one-time cleanup workflow.

---

## What Changed

### Triggers

| Workflow | Before | After |
|---|---|---|
| `android-ci.yml` | push to main, PR to main | unchanged (already correct) |
| `nightly.yml` | `workflow_dispatch` | unchanged (no cron was present) |
| `release.yml` | `push: tags: v*.*.*` | unchanged (tag trigger is intentional) |
| `cleanup-history.yml` | — | **new**, `workflow_dispatch` only |

No cron/schedule triggers exist in any workflow.

### Concurrency

All workflows now have a `concurrency` block:

```yaml
# android-ci.yml — cancel old runs on same branch/PR
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

# nightly.yml — never cancel a nightly in progress
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: false

# release.yml — never cancel a release in progress
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: false
```

### Artifact Retention

All pipeline artifacts set to `retention-days: 1`.

| Workflow | Artifact | Before | After |
|---|---|---|---|
| `android-ci.yml` | lint-report, unit-test-results, debug-apk | 1 day | 1 day |
| `nightly.yml` | all nightly artifacts | 1 day | 1 day |
| `release.yml` | release-apk, release-aab, proguard-mapping | 90 / 180 days | **1 day** |

> **Note:** Files attached to GitHub Releases (via `softprops/action-gh-release`) are permanent. The `retention-days` setting only affects intermediate pipeline artifacts used to pass files between jobs within the same run.

### Cleanup Jobs

Every workflow now ends with a `cleanup` job that:

- Runs `if: always()` (even if earlier jobs fail)
- Requires `permissions: actions: write`
- Uses GitHub REST API via `curl` (no `gh` CLI dependency)
- Paginates through all completed runs of the same workflow and deletes all except the current run
- Paginates through all artifacts and deletes all except those belonging to the current run

### Version Matrix

No matrix exists in any workflow. `android-ci.yml` uses a single API level configuration. Instrumented tests run at API 28 (`target: default`, `arch: x86`) in the nightly workflow — the most stable combination on GitHub-hosted runners.

---

## Checklist

### android-ci.yml

- [x] Trigger: `push: [main]` + `pull_request: [main]` only
- [x] No cron/schedule
- [x] `concurrency: cancel-in-progress: true`
- [x] All artifacts `retention-days: 1`
- [x] `cleanup` job with `actions: write`
- [x] Cleanup deletes old runs via `DELETE /repos/{repo}/actions/runs/{id}`
- [x] Cleanup deletes old artifacts via `DELETE /repos/{repo}/actions/artifacts/{id}`
- [x] No version matrix

### nightly.yml

- [x] Trigger: `workflow_dispatch` only
- [x] No cron/schedule
- [x] `concurrency: cancel-in-progress: false`
- [x] All artifacts `retention-days: 1`
- [x] Instrumented tests: API 28, `target: default`, `arch: x86`
- [x] Retry step on instrumented test failure
- [x] `cleanup` job with `actions: write`
- [x] No version matrix

### release.yml

- [x] Trigger: `push: tags: v*.*.*` (intentional — release only on version tag)
- [x] No cron/schedule
- [x] `concurrency: cancel-in-progress: false`
- [x] All pipeline artifacts `retention-days: 1`
- [x] GitHub Release assets remain permanent (uploaded via `softprops/action-gh-release`)
- [x] `cleanup` job with `actions: write`

### cleanup-history.yml

- [x] Trigger: `workflow_dispatch` only
- [x] `dry_run` input to preview deletions without executing
- [x] Deletes ALL completed runs across ALL workflows (except current)
- [x] Deletes ALL pipeline artifacts (except current run)
- [x] `permissions: actions: write`

---

## How to Run One-Time Cleanup

1. Go to **Actions → Cleanup History (One-Time)** in the GitHub UI
2. Click **Run workflow**
3. Optional: set `dry_run: true` first to preview what will be deleted
4. Run again with `dry_run: false` to actually delete

After running once, the per-workflow `cleanup` jobs maintain a clean state automatically.

---

## Secrets Required

| Secret | Used by | Purpose |
|---|---|---|
| `GITHUB_TOKEN` | all workflows | Automatically provided by Actions |
| `RELEASE_KEYSTORE_BASE64` | `release.yml` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | `release.yml` | Keystore password |
| `KEY_ALIAS` | `release.yml` | Key alias in keystore |
| `KEY_PASSWORD` | `release.yml` | Key password |

No secrets are printed to logs. `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` are passed only as environment variables to Gradle, not echoed. The decoded keystore file is deleted in a `if: always()` cleanup step.
