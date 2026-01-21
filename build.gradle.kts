import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java") // Java support
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    // IntelliJ Platform Gradle Plugin
    id("org.jetbrains.intellij.platform") version "2.10.5"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.2.1"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "2025.3.1"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    // Apache RAT Plugin
    id("org.nosphere.apache.rat") version "0.8.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

// Configure Java compiler options
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        intellijIdea(version)

        // Plugin Dependencies -> https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.javaee")
        bundledPlugin("com.intellij.javaee.web")
        bundledPlugin("com.intellij.jsp")
        bundledPlugin("com.intellij.spring")
        bundledPlugin("com.intellij.java-i18n")
        bundledPlugin("com.intellij.freemarker")
        bundledPlugin("com.intellij.velocity")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("JavaScript")

        pluginVerifier()
        zipSigner()

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.JUnit5)
        testFramework(TestFrameworkType.Bundled)
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

java.sourceSets["main"].java {
    srcDir("src/main/gen")
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }

    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
        // Mute warnings about plugin ID prefix - this plugin was donated by JetBrains
        // and retains its original ID for backwards compatibility
        freeArgs = listOf(
            "-mute", "ForbiddenPluginIdPrefix",
            "-mute", "TemplateWordInPluginId"
        )
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    rat {
        // Input directory, defaults to '.'
        inputDir.set(file("src/main/java"))
        inputDir.set(file("src/main/resources"))
        inputDir.set(file("src/test/java"))
        inputDir.set(file("src/test/testData"))

        // List of Gradle exclude directives, defaults to ['**/.gradle/**']
        excludes.add("**/build/**")
        excludes.add("src/test/testData/**")

        // Fail the build on rat errors, defaults to true
        failOnError.set(false)

        // Prints the list of files with unapproved licences to the console, defaults to false
        verbose.set(true)
    }
}

val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
  task {
    jvmArgumentProviders += CommandLineArgumentProvider {
      listOf(
        "-Drobot-server.port=8082",
        "-Dide.mac.message.dialogs.as.sheets=false",
        "-Djb.privacy.policy.text=<!--999.999-->",
        "-Djb.consents.confirmation.enabled=false",
      )
    }
  }

  plugins {
    robotServerPlugin()
  }
}
