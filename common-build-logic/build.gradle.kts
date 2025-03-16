
plugins {
    `groovy-gradle-plugin`
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    //NOTE: when updating intellij-platform-gradle-plugin update also org.jetbrains.intellij.platform.settings plugin
    // in settings file to the same version. we need to maintain it in two places, unfortunately.
    implementation("org.jetbrains.intellij.platform:intellij-platform-gradle-plugin:2.3.0") // Update also org.jetbrains.intellij.platform.settings in settings.gradle.kts
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
    implementation("com.glovoapp.gradle:versioning:1.1.10")
    implementation("de.undercouch:gradle-download-task:5.6.0")
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    jvmToolchain {
        this.languageVersion.set(JavaLanguageVersion.of("17"))
    }
}


tasks {


    test {
        testLogging {
            events("FAILED")
            showStandardStreams = true
        }

        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) { // root suite
                    logger.lifecycle("----")
                    logger.lifecycle("Test result: ${result.resultType}")
                    logger.lifecycle(
                        "Test summary: ${result.testCount} tests, " +
                                "${result.successfulTestCount} succeeded, " +
                                "${result.failedTestCount} failed, " +
                                "${result.skippedTestCount} skipped"
                    )

                }
            }
        })
    }
}
