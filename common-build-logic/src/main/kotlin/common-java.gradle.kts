import common.currentProfile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(project.currentProfile().javaVersion))
        //it is possible to use jetbrains runtime for toolchain but then
        // runIde will use this runtime to run the development instance and that is not ideal,
        // it's better to run the development instance with the bundled runtime.
        // so IMO its not a good idea to configure 'vendor = JvmVendorSpec.JETBRAINS'
        // follow this issue: https://github.com/JetBrains/gradle-intellij-plugin/issues/1538
        vendor = JvmVendorSpec.ADOPTIUM
    }
}


tasks {

    withType<JavaCompile> {

        doFirst {
            logger.lifecycle("${name}:Compiling java with release:${options.release.get()}, compiler:${javaCompiler.get().executablePath}")
        }

        options.compilerArgs.addAll(listOf("-Xlint:unchecked,deprecation"))
        options.release.set(JavaLanguageVersion.of(project.currentProfile().javaVersion).asInt())
    }


    withType<Test> {
        doFirst {
            logger.lifecycle("${name}: Testing with {}", javaLauncher.get().executablePath)
        }
        useJUnitPlatform()

        addTestListener(object : TestListener {
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}

            override fun beforeSuite(suite: TestDescriptor) {
                if (suite.parent == null) { // root suite
                    logger.lifecycle("Starting Test suite {}", suite)
                }
            }

            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) { // root suite
                    logger.lifecycle(
                        "Test suite ${suite.name} completed:: ${result.resultType}, " +
                                "success ${result.successfulTestCount}, " +
                                "failed ${result.failedTestCount}, " +
                                "skipped ${result.skippedTestCount}."
                    )

                }
            }
        })

        testLogging {
            showStandardStreams = false
            lifecycle {
                events = mutableSetOf(TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.SHORT
                showExceptions = true
                showCauses = true
                showStackTraces = false
                showStandardStreams = false
            }
            debug {
                events = mutableSetOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
                exceptionFormat = TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
                showStandardStreams = true
            }
            info.events = debug.events
            info.exceptionFormat = debug.exceptionFormat
        }

        addTestOutputListener { testDescriptor, outputEvent ->
            if (outputEvent.destination == TestOutputEvent.Destination.StdErr) {
                logger.error("Test: " + testDescriptor + ", error: " + outputEvent.message)
            }
        }
    }
}


