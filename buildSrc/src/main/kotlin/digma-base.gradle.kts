import common.properties
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
    id("jvm-test-suite")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(properties("javaVersion", project)))
    }
}

group = properties("pluginGroup", project)
version = common.semanticversion.getSemanticVersion(project)

tasks.register("printSemanticVersion") {
    doLast {
        println("${project.name} ${common.semanticversion.getSemanticVersion(project)}")
    }
}


repositories {
    mavenCentral()
    // jetbrains artifacts repositories
    maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-repository/snapshots")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

configurations {
    all {
        // Allows using project dependencies instead of IDE dependencies during compilation and test running
        resolutionStrategy {
            sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
        }
    }
}


project.afterEvaluate {
    tasks {
        //the project can be built with build task or buildPlugin task. build triggers buildPlugin.
        //but buildPlugin doesn't trigger build, so if calling only buildPlugin unit tests will not run.
        //if the project is built by calling buildPlugin we want it to trigger test.
        named("buildPlugin") {
            dependsOn(test)
        }
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
        options.compilerArgs.addAll(listOf("-Xlint:unchecked,deprecation"))
        options.release.set(JavaLanguageVersion.of(properties("javaVersion", project)).asInt())
    }


    //configuration of test tasks logging
    withType<Test> {
        doFirst {
            logger.lifecycle("${project.name}:${name}: testing java with {}", javaLauncher.get().executablePath)
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


