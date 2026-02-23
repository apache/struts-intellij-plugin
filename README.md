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
This is a plugin to support development of Apache Struts based web applications using IntelliJ IDEA.
<!-- Plugin description end -->

## Documentation

Questions related to the usage of the plugin should be posted to the [user mailing list](https://struts.apache.org/mail.html).
Any issues should be reported using JIRA and [IDEA plugin](https://issues.apache.org/jira/issues/?jql=project%20%3D%20WW%20AND%20component%20%3D%20%22IDEA%20Plugin%22) component.

## Testing

[Testing plugins][docs:testing-plugins] is an essential part of the plugin development to make sure that everything works as expected between IDE releases and plugin refactorings.
The IntelliJ Platform Plugin Template project provides integration of two testing approaches – functional and UI tests.

### Functional tests

Most of the IntelliJ Platform codebase tests are model-level, run in a headless environment using an actual IDE instance.
The tests usually test a feature as a whole rather than individual functions that comprise its implementation, like in unit tests.

In `src/test/kotlin`, you'll find a basic `MyPluginTest` test that utilizes `BasePlatformTestCase` and runs a few checks against the XML files to indicate an example operation of creating files on the fly or reading them from `src/test/testData/rename` test resources.

> **Note**
>
> Run your tests using predefined *Run Tests* configuration or by invoking the `./gradlew check` Gradle task.

### Code coverage

The [Kover][gh:kover] – a Gradle plugin for Kotlin code coverage agents: IntelliJ and JaCoCo – is integrated into the project to provide the code coverage feature.
Code coverage makes it possible to measure and track the degree of plugin sources testing.
The code coverage gets executed when running the `check` Gradle task.
The final test report is sent to [CodeCov][codecov] for better results visualization.

### UI tests

If your plugin provides complex user interfaces, you should consider covering them with tests and the functionality they utilize.

[IntelliJ UI Test Robot][gh:intellij-ui-test-robot] allows you to write and execute UI tests within the IntelliJ IDE running instance.
You can use the [XPath query language][xpath] to find components in the currently available IDE view.
Once IDE with `robot-server` has started, you can open the `http://localhost:8082` page that presents the currently available IDEA UI components hierarchy in HTML format and use a simple `XPath` generator, which can help test your plugin's interface.

> **Note**
>
> Run IDE for UI tests using predefined *Run IDE for UI Tests* and then *Run Tests* configurations or by invoking the `./gradlew runIdeForUiTests` and `./gradlew check` Gradle tasks.

Check the UI Test Example project you can use as a reference for setting up UI testing in your plugin: [intellij-ui-test-robot/ui-test-example][gh:ui-test-example].

```kotlin
class MyUITest {

  @Test
  fun openAboutFromWelcomeScreen() {
    val robot = RemoteRobot("http://127.0.0.1:8082")
    robot.find<ComponentFixture>(byXpath("//div[@myactionlink = 'gearHover.svg']")).click()
    // ...
  }
}
```

A dedicated [Run UI Tests](.github/workflows/run-ui-tests.yml) workflow is available for manual triggering to run UI tests against three different operating systems: macOS, Windows, and Linux.
Due to its optional nature, this workflow isn't set as an automatic one, but this can be easily achieved by changing the `on` trigger event, like in the [Build](.github/workflows/build.yml) workflow file.

## Qodana integration

To increase the project value, the IntelliJ Platform Plugin Template got integrated with [Qodana][jb:qodana], a code quality monitoring platform that allows you to check the condition of your implementation and find any possible problems that may require enhancing.

Qodana brings into your CI/CD pipelines all the smart features you love in the JetBrains IDEs and generates an HTML report with the actual inspection status.

Qodana inspections are accessible within the project on two levels:

- using the [Qodana IntelliJ GitHub Action][jb:qodana-github-action], run automatically within the [Build](.github/workflows/build.yml) workflow,
- with the [Gradle Qodana Plugin][gh:gradle-qodana-plugin], so you can use it on the local environment or any CI other than GitHub Actions.

Qodana inspection is configured with the `qodana { ... }` section in the Gradle build file and [`qodana.yml`][file:qodana.yml] YAML configuration file.

> **Note**
>
> Qodana requires Docker to be installed and available in your environment.

To run inspections, you can use a predefined *Run Qodana* configuration, which will provide a full report on `http://localhost:8080`, or invoke the Gradle task directly with the `./gradlew runInspections` command.

A final report is available in the `./build/reports/inspections/` directory.

## Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug configurations* that expose corresponding Gradle tasks:

| Configuration name   | Description                                                                                                                                                                   |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run Plugin           | Runs [`:runIde`][gh:gradle-intellij-plugin-runIde] Gradle IntelliJ Plugin task. Use the *Debug* icon for plugin debugging.                                                    |
| Run Verifications    | Runs [`:runPluginVerifier`][gh:gradle-intellij-plugin-runPluginVerifier] Gradle IntelliJ Plugin task to check the plugin compatibility against the specified IntelliJ IDEs.   |
| Run Tests            | Runs [`:test`][gradle:lifecycle-tasks] Gradle task.                                                                                                                           |
| Run IDE for UI Tests | Runs [`:runIdeForUiTests`][gh:intellij-ui-test-robot] Gradle IntelliJ Plugin task to allow for running UI tests within the IntelliJ IDE running instance.                     |
| Run Qodana           | Runs [`:runInspections`][gh:gradle-qodana-plugin] Gradle Qodana Plugin task. Starts Qodana inspections in a Docker container and serves generated report on `localhost:8080`. |

> **Note**
>
> You can find the logs from the running task in the `idea.log` tab.
>
> ![Run/Debug configuration logs][file:run-logs.png]

### Changelog maintenance

When releasing an update, it is essential to let your users know what the new version offers.
The best way to do this is to provide release notes.

The changelog is a curated list that contains information about any new features, fixes, and deprecations.
When they're provided, these lists are available in a few different places:
- the [CHANGELOG.md](./CHANGELOG.md) file,
- the [Releases page][gh:releases],
- the *What's new* section of JetBrains Marketplace Plugin page,
- and inside the Plugin Manager's item details.

There are many methods for handling the project's changelog.
The one used in the current template project is the [Keep a Changelog][keep-a-changelog] approach.

The [Gradle Changelog Plugin][gh:gradle-changelog-plugin] takes care of propagating information provided within the [CHANGELOG.md](./CHANGELOG.md) to the [Gradle IntelliJ Plugin][gh:gradle-intellij-plugin].
You only have to take care of writing down the actual changes in proper sections of the `[Unreleased]` section.

You start with an almost empty changelog:

```
# YourPlugin Changelog

## [Unreleased]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
```

Now proceed with providing more entries to the `Added` group, or any other one that suits your change the most (see [How do I make a good changelog?][keep-a-changelog-how] for more details).

When releasing a plugin update, you don't have to care about bumping the `[Unreleased]` header to the upcoming version – it will be handled automatically on the Continuous Integration (CI) after you publish your plugin.
GitHub Actions will swap it and provide you an empty section for the next release so that you can proceed with your development:

```
# YourPlugin Changelog

## [Unreleased]

## [0.0.1]
### Added
- An awesome feature
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

### Fixed
- One annoying bug
```

To configure how the Changelog plugin behaves, i.e., to create headers with the release date, see [Gradle Changelog Plugin][gh:gradle-changelog-plugin] README file.

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
