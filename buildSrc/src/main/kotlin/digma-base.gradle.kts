import common.properties
import gradle.kotlin.dsl.accessors._32850698d653820f6f0bff6e9d585ccb.build
import gradle.kotlin.dsl.accessors._744ce3376bf13942cde73334c3c9bc84.kotlin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins{
    `java`
    `jvm-test-suite`
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
            useJUnitJupiter("5.8.2")

            dependencies {
                implementation(project)

                //this is a workaround to an issue with junit launcher in intellij platform 2022.2 plus gradle 7.5.1.
                //it is discussed in a slack thread and will probably be fixed in the next intellij platform patch.
                //todo: when upgrading the platform version check if its fixed just by removing it and building the project with no errors.
                //https://jetbrains-platform.slack.com/archives/CPL5291JP/p1660085792256189
                runtimeOnly("org.junit.platform:junit-platform-launcher")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
            }
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
