# IntelliJ IDEA 2026.2 (262) Compatibility

**Date:** 2026-07-24  
**Status:** Draft for review

## Problem

JetBrains Marketplace reports that the Apache Struts IntelliJ plugin is not compatible with the recently released IDE version **262** (IntelliJ IDEA **2026.2**).

The plugin is currently pinned to 2026.1 only:

| Property | Current value |
|---|---|
| `pluginVersion` | `261.19027.1` |
| `pluginSinceBuild` | `261` |
| `pluginUntilBuild` | `261.*` |
| `platformVersion` | `2026.1` |
| JVM toolchain / CI | Java 21 |

Users on IDEA 2026.2 cannot install or update the plugin until the compatibility range and build target are advanced.

## Goals

1. Make the plugin installable on IntelliJ IDEA **2026.2** (build branch **262**).
2. Build and verify against `platformVersion = 2026.2`.
3. Compile and run CI with **Java 25** (required for 2026.2+ per JetBrains Platform docs).
4. Keep `./gradlew test -x rat` and plugin verifier green for the enabled test suite.
5. Document the bump in `CHANGELOG.md` and the platform mapping in `CLAUDE.md`.

## Non-Goals

- Supporting 2026.1 alongside 2026.2 in the same plugin build.
- Cutting a Marketplace release (prepare/publish) as part of this work.
- Re-enabling historically disabled / underscore-prefixed tests unless they block the upgrade.
- Unrelated feature work or opportunistic refactors.

## Decision

Perform a sequential “checklist” platform upgrade targeting **2026.2 only**, following the repository’s existing Platform Upgrade Checklist.

This matches the previous 261-only bump: change version/compatibility properties, align Java tooling, verify with build → plugin verifier → unit tests, fix only what 262 breaks, then document.

## Alternatives Considered

### Dual-range support (261–262.*)

Widen `pluginUntilBuild` to cover both 2026.1 and 2026.2 while building against the lowest supported platform.

Rejected because the user chose 262-only coverage, and 2026.2’s Java 25 requirement makes a dual-version range harder to maintain if the build must stay on the lowest supported JDK.

### Verifier-first, tests later

Bump versions and chase Marketplace verifier failures before addressing unit tests.

Rejected as the primary process: useful as an ordering nuance inside the checklist, but treating verifier as the only gate leaves the branch red longer and misses test-infra breakages that have been common in past platform bumps.

### Dual-branch / compatibility shim

Keep a 261 release line and a separate 262 line with conditional APIs.

Rejected as out of scope for a routine compatibility bump; published 2026.2 incompatible APIs (PolySymbols renames, K1 removal) do not appear used by this plugin.

## Version and Java Configuration

### `gradle.properties`

| Property | From | To |
|---|---|---|
| `pluginVersion` | `261.19027.1` | `262.<BUILD>.1` (branch prefix only; BUILD/FIX scheme unchanged) |
| `pluginSinceBuild` | `261` | `262` |
| `pluginUntilBuild` | `261.*` | `262.*` |
| `platformVersion` | `2026.1` | `2026.2` |

If JetBrains’ official build-number table or plugin verifier disagrees with branch `262` at implementation time, use the official source of truth and adjust `pluginSinceBuild` / `pluginUntilBuild` / version prefix accordingly. Do not invent branch numbers.

### Java 25 alignment

- `build.gradle.kts`: `jvmToolchain(25)`
- GitHub Actions (`build.yml`, `nightly.yml`, `prepare_release.yml`, `release.yml`): `java-version: 25`
- `qodana.yml`: `projectJDK: 25`; bump the Qodana linter image to a 2026.2-compatible tag when available. If no suitable image exists yet, keep the current 2026.1 image only if CI still accepts it, and document that follow-up.

### Dependency bumps

Leave IntelliJ Platform Gradle Plugin, Gradle, and related tooling versions unchanged unless compile, tests, or verifier require a bump. If a bump is required, use the minimum working version and record it in `CHANGELOG.md`.

## Verification and Fix Strategy

Execute in this order:

1. Apply version and Java configuration changes.
2. `./gradlew build` — fix compile/API breaks only as needed.
3. `./gradlew runPluginVerifier` against 2026.2 — fix Marketplace-blocking issues.
4. `./gradlew test -x rat` — fix regressions from 262 / Java 25 in currently enabled tests.
5. Update `CHANGELOG.md` and `CLAUDE.md`.

### Expected risk

Published IntelliJ Platform 2026.2 incompatible changes focus on PolySymbols renames and Kotlin K1 removal. This plugin does not appear to use those APIs, so most work should be configuration and tooling, plus any deprecations/removals or test-infra quirks surfaced by the new platform (similar to prior bumps).

### Disabled-test policy

If a failure is clearly the same historical “disabled for older platform” debt (underscore-prefixed methods / placeholder tests), leave it disabled and note it. Only fix currently enabled tests that fail on 262.

## Components Touched

| Unit | Role |
|---|---|
| `gradle.properties` | Platform version, compatibility range, plugin version prefix |
| `build.gradle.kts` | Java 25 toolchain; optional tooling bump if required |
| `.github/workflows/*` | CI JDK 25 |
| `qodana.yml` | `projectJDK` / linter alignment |
| Production or test sources | Only if 262 forces compile, test, or verifier fixes |
| `CHANGELOG.md`, `CLAUDE.md` | Document the platform/Java bump and version mapping |

No new modules or extension points are introduced.

## Delivery

- Work on a short-lived branch (for example `build/intellij-2026-2`).
- Deliver a merge-ready PR to `main`.
- Do **not** run prepare/publish release workflows as part of this work; release separately later.
- After merge, Marketplace compatibility for 262 becomes available once a 262.x plugin version is published through the normal release process.

## Success Criteria

- Project builds on Java 25 against IntelliJ IDEA 2026.2.
- Plugin descriptor / patched XML advertises `since-build`/`until-build` covering 262 / `262.*`.
- Plugin verifier accepts the 262 compatibility range (no blocking incompatibilities for the targeted IDE).
- Enabled unit tests pass via `./gradlew test -x rat`.
- `CHANGELOG.md` records the platform and Java upgrades.
- `CLAUDE.md` platform table includes 262.x → 2026.2.

## Out of Scope Follow-ups

- Publishing the 262.x Marketplace release.
- Re-enabling disabled OGNL/structure/Spring/JSP reference tests.
- Expanding compatibility back to 2026.1.
