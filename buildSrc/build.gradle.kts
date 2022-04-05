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
    //some issue with KotlinCompile needs that, usually that's not necessary
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
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
                logger.lifecycle("Test summary: ${result.testCount} tests, " +
                        "${result.successfulTestCount} succeeded, " +
                        "${result.failedTestCount} failed, " +
                        "${result.skippedTestCount} skipped")

            }
        }
    })
}



tasks{

    withType<JavaCompile> {
        options.release.set(11)
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    jar{
        //don't need the jar unless there is at least one gradlePlugin
        enabled = false
    }
}
