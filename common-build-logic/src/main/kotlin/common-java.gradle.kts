import common.currentProfile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
    id("jvm-test-suite")
}

//todo: check with JvmVendorSpec.JETBRAINS
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(project.currentProfile().javaVersion))
        //it is possible to use jetbrains runtime for toolchain but then
        // runIde will use this runtime to run the development instance and that is not ideal,
        // it's better to run the development instance with the bundled runtime.
        // so IMO its not a good idea to configure 'vendor = JvmVendorSpec.JETBRAINS'
        // follow this issue: https://github.com/JetBrains/gradle-intellij-plugin/issues/1538

        vendor = JvmVendorSpec.AMAZON
    }
}


testing {
    //https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html

    //this is the basic configuration of the test task for all projects.
    //it can be extended in projects to add dependencies and other configuration.
    //see for example analytics-provider project.
    //jvmTestSuite eventually adds a test task to every project so the test task can also be configured
    //independently like in legacy gradle versions. for example just adding testImplementation dependencies.
    //but extending jvmTestSuite like in analytics-provider is more consice and safe with regards to depenency hell.
    //so all projects are already configured to use junit jupiter.

    suites {
        val test by getting(JvmTestSuite::class) {
            //this is the only place junit version should be mentioned in the project.
            //it applies to all projects. can't use versions catalog in scripts plugins so using
            //hard coded version.
            useJUnitJupiter()

            dependencies {
                implementation(project())
            }
        }
    }
}



tasks {

    withType<JavaCompile> {

        doFirst {
            logger.lifecycle("Compiling java with release:${options.release.get()}, compiler:${javaCompiler.get().executablePath}")
        }

        options.compilerArgs.addAll(listOf("-Xlint:unchecked,deprecation"))
        options.release.set(JavaLanguageVersion.of(project.currentProfile().javaVersion).asInt())
    }


    //configuration of test tasks logging
    withType<Test> {
        doFirst {
            logger.lifecycle("${project.name}:${name}: Testing java with {}", javaLauncher.get().executablePath)
        }


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


