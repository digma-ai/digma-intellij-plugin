plugins {
    id("common-java-library")
    `jvm-test-suite`
}


dependencies {
    implementation(project(":model"))
    implementation(libs.retrofit.client)
    implementation(libs.retrofit.jackson)
    implementation(libs.guava)
}


testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.get())

            dependencies {
                implementation(project)
                implementation(libs.kotlin.stdlib.jdk8)
                implementation(libs.okhttp)
                implementation(libs.okhttp.mockwebserver)
            }

            targets {
                all {
                    testTask.configure {
                        setForkEvery(1L) //because MockWebServer is static
                    }
                }
            }
        }
    }
}