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

                        //this is only to silent the warning : see https://stackoverflow.com/questions/60915381/retrofit2-maven-project-illegal-reflective-access-warning
                        //WARNING: Illegal reflective access by retrofit2.Platform (file:/home/shalom/.gradle/caches/modules-2/files-2.1/com.squareup.retrofit2/retrofit/2.9.0/d8fdfbd5da952141a665a403348b74538efc05ff/retrofit-2.9.0.jar) to constructor java.lang.invoke.MethodHandles$Lookup(java.lang.Class,int)
                        jvmArgs = listOf("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED")
                    }
                }
            }
        }
    }
}