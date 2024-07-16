plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.nosphere.apache.rat") version "0.8.1"
}

group = "com.intellij"
version = "2023.3"

repositories {
    mavenCentral()
}

java.sourceSets["main"].java {
    srcDir("src/main/gen")
}

intellij {
    version.set("2023.1.1")
    type.set("IU") // Target IDE Platform

    plugins.set(listOf(
        "com.intellij.javaee",
        "com.intellij.javaee.web",
        "com.intellij.spring",
        "com.intellij.freemarker",
        "com.intellij.velocity",
        "org.intellij.groovy",
        "JavaScript",
        "com.intellij.java-i18n"
    ))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("233.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    rat {
        // Input directory, defaults to '.'
        inputDir.set(file("src/main/java"))
        inputDir.set(file("src/main/resources"))
        inputDir.set(file("src/test/java"))
        inputDir.set(file("src/test/testData"))

        // List of Gradle exclude directives, defaults to ['**/.gradle/**']
        excludes.add("**/build/**")
        excludes.add("src/test/testData/**/*.txt")
        excludes.add("src/test/testData/**")

        // Fail the build on rat errors, defaults to true
        failOnError.set(false)

        // Prints the list of files with unapproved licences to the console, defaults to false
        verbose.set(true)
    }
}
