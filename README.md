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

Record changes under the `[Unreleased]` section in `CHANGELOG.md`. The CI pipeline handles version bumping on release.

### Release process

The plugin uses a two-phase release process with nightly builds for continuous delivery:

**Nightly builds** are created automatically when commits are merged to `main`. The [Build](.github/workflows/build.yml) workflow runs tests, builds the plugin, and publishes it to the JetBrains Marketplace **nightly** channel. A GitHub pre-release is also created with the plugin zip attached.

**Preparing a release** is a manual step. Go to **Actions → Prepare Release → Run workflow**, optionally providing a version override. This workflow:
1. Builds the plugin (using the version from `gradle.properties` or your override)
2. Creates a git tag `v{VERSION}` and pushes it
3. Creates a GitHub **pre-release** with the plugin zip attached

PMC members can then download the zip from the pre-release, test it locally, and vote.

**Publishing a release** happens when the pre-release is promoted to a full release. Edit the GitHub pre-release, uncheck **"Set as a pre-release"**, and save. This triggers the [Release](.github/workflows/release.yml) workflow which:
1. Publishes the plugin to the JetBrains Marketplace **Stable** channel
2. Uploads the plugin zip as a release asset
3. Creates a pull request to update the changelog
