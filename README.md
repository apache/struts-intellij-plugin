<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
Apache Struts IntelliJ IDEA plugin
-------------------------------

[![Build](https://github.com/apache/struts-intellij-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/apache/struts-intellij-plugin/actions/workflows/build.yml)

<!-- Plugin description -->
Provides full integration of Apache Struts 2.
<!-- Plugin description end -->

## Documentation

Questions related to the usage of the plugin should be posted to the [user mailing list](https://struts.apache.org/mail.html).
Any issues should be reported using JIRA and [IDEA plugin](https://issues.apache.org/jira/issues/?jql=project%20%3D%20WW%20AND%20component%20%3D%20%22IDEA%20Plugin%22) component.

## Testing

Tests are located in `src/test/java` and use IntelliJ Platform test frameworks (`LightJavaCodeInsightFixtureTestCase` and similar). Test data fixtures are in `src/test/testData`.

```bash
./gradlew test -x rat                        # Run tests (excluding Apache RAT license checks)
./gradlew test --tests "OgnlParsingTest"     # Run a specific test class
./gradlew test --tests "*Resolving*"         # Run tests matching a pattern
```

### Code coverage

[Kover](https://github.com/Kotlin/kotlinx-kover) is integrated for code coverage. Generate an HTML report with:

```bash
./gradlew koverHtmlReport                    # Report in build/reports/kover/
```

### Changelog maintenance

The project follows the [Keep a Changelog](https://keepachangelog.com) approach. The [Gradle Changelog Plugin](https://github.com/JetBrains/gradle-changelog-plugin) propagates entries from [CHANGELOG.md](./CHANGELOG.md) to the JetBrains Marketplace plugin page.

Record changes under the `[Unreleased]` section in `CHANGELOG.md`. The release workflow moves that section into a versioned entry and bumps `pluginVersion` in `gradle.properties` for the next cycle.

### Release process

The plugin uses a two-phase release process: a **release candidate** for PMC review, then promotion to a **stable** Marketplace release. Nightly builds provide continuous delivery between stable releases.

#### Version numbering

The plugin version in `gradle.properties` follows `{BRANCH}.{BUILD}.{FIX}` (e.g. `261.19027.1`):

| Segment | Meaning | Example |
|---------|---------|---------|
| BRANCH | IntelliJ platform branch | `261` = 2026.1 |
| BUILD | Monotonic build counter | `19027` |
| FIX | Patch within a release | `1` |

Related properties: `platformVersion`, `pluginSinceBuild`, and `pluginUntilBuild` define the supported IDE range. You normally do not bump `pluginVersion` manually before a release — the post-release workflow advances BUILD for the next cycle.

#### Continuous integration

The [Build](.github/workflows/build.yml) workflow runs on every pull request and on pushes to `main`. It builds the plugin, runs tests, Qodana inspections, and Plugin Verifier checks. Pull requests get a comment with a downloadable plugin artifact for manual testing. This workflow does **not** publish to the Marketplace.

#### Nightly builds

The [Nightly](.github/workflows/nightly.yml) workflow runs on a daily schedule (02:00 UTC) and can be triggered manually. If there are new commits since the last nightly tag, it:

1. Builds the plugin with a version like `261.19027-nightly.3`
2. Publishes to the JetBrains Marketplace **nightly** channel
3. Creates a git tag (`v261.19027-nightly.3`)

Nightly tags are also used when calculating the BUILD increment after a stable release.

#### Phase 1 — Prepare a release candidate (manual)

Go to **Actions → Prepare Release → Run workflow** ([prepare_release.yml](.github/workflows/prepare_release.yml)), optionally providing a version override. If omitted, the version from `gradle.properties` is used. This workflow:

1. Builds the plugin (`./gradlew buildPlugin`)
2. Extracts unreleased changelog entries for the release notes
3. Creates and pushes a git tag `v{VERSION}`
4. Creates a GitHub **pre-release** with the plugin zip attached

It does **not** publish to the JetBrains Marketplace stable channel.

PMC members download the zip from the pre-release and install it via **Settings → Plugins → ⚙️ → Install Plugin from Disk…**, then test and vote.

#### Phase 2 — Publish a stable release

When voting passes, promote the GitHub pre-release to a full release: edit the release, uncheck **"Set as a pre-release"**, and save. This triggers the [Release](.github/workflows/release.yml) workflow, which:

1. Publishes the plugin to the JetBrains Marketplace **Stable** channel (the zip attached during Prepare Release is the artifact PMC voted on; it is not re-uploaded)
2. Patches `CHANGELOG.md` (moves `[Unreleased]` → released version)
3. Bumps `pluginVersion` in `gradle.properties` for the next release cycle (BUILD + number of nightly tags since the previous release, minimum +1)
4. Opens a **post-release pull request** with the changelog patch and version bump

Merge the post-release PR to complete the release cycle.

#### Pre-release checklist

1. Ensure `[Unreleased]` in `CHANGELOG.md` is complete
2. Confirm `pluginVersion` and platform properties in `gradle.properties` are correct
3. Ensure `main` is green (required checks: Test, Verify plugin)
4. Run **Prepare Release**
5. Share the GitHub pre-release with PMC for testing and voting
6. After the vote: promote pre-release → full release
7. Review and merge the auto-created post-release PR
