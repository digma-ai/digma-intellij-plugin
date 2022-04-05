import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("org.jetbrains.intellij.plugins:gradle-intellij-plugin:1.5.2")
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

/*
todo: upgrade gradle to 7.5
this warning can be ignored:
'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
gradle issue:
https://github.com/gradle/gradle/issues/18935
 */

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