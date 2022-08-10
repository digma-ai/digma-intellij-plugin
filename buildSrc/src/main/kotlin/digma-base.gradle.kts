import common.properties
import gradle.kotlin.dsl.accessors._32850698d653820f6f0bff6e9d585ccb.build
import gradle.kotlin.dsl.accessors._744ce3376bf13942cde73334c3c9bc84.kotlin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins{
    `java`
    id("com.dorongold.task-tree")
    id("com.glovoapp.semantic-versioning")
}

semanticVersion{
    //if the propertiesFile is not changed the plugin will look for a file in each module.
    propertiesFile.set(project.rootProject.file("version.properties"))
}
tasks.incrementSemanticVersion{
    //disable the task for all projects.
    //because digma-base is applied to all projects then calling incrementSemanticVersion will be invoked
    //for each project and we don't want that.
    //we could apply the plugin only to the main project but then we can't use its tasks in script plugins.
    //the task is enabled only in the main project.
    enabled = false
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(properties("javaVersion",project)))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

group = properties("pluginGroup",project)
version = project.semanticVersion.version.get()

repositories {
    mavenCentral()
}

configurations {
    all {
        // Allows using project dependencies instead of IDE dependencies during compilation and test running
        resolutionStrategy {
            sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
        }
    }
}


project.afterEvaluate{
    tasks{
        //the project can be built with build task or buildPlugin task. build triggers buildPlugin.
        //but buildPlugin doesn't trigger build, so if calling only buildPlugin unit tests will not run.
        //if the project is built by calling buildPlugin we want it to trigger test.
        named("buildPlugin"){
            dependsOn(test)
        }
    }
}


tasks{

    withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked,deprecation"))
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
