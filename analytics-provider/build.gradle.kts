@file:Suppress("UnstableApiUsage")

plugins {
    id("common-java-library")
    id("jvm-test-suite")
}


dependencies {
    implementation(project(":model"))
    implementation(libs.retrofit.client)
    implementation(libs.retrofit.jackson)
    implementation(libs.retrofit.scalars)
    implementation(libs.guava)
}


testing {
    //basic shared configuration is in buildSrc/src/main/kotlin/digma-base.gradle.kts

    suites {
        val test by getting(JvmTestSuite::class) {

            dependencies {
                implementation(project())
                implementation(libs.kotlin.stdlib.jdk8)
                implementation(libs.okhttp)
                implementation(libs.okhttp.mockwebserver)
                implementation(libs.google.gson)
            }

            targets {
                all {
                    testTask.configure {
                        forkEvery = 1L //because MockWebServer is static
//                        setForkEvery(1L)

                        //this is only to silent the warning : see https://stackoverflow.com/questions/60915381/retrofit2-maven-project-illegal-reflective-access-warning
                        //WARNING: Illegal reflective access by retrofit2.Platform (file:/home/shalom/.gradle/caches/modules-2/files-2.1/com.squareup.retrofit2/retrofit/2.9.0/d8fdfbd5da952141a665a403348b74538efc05ff/retrofit-2.9.0.jar) to constructor java.lang.invoke.MethodHandles$Lookup(java.lang.Class,int)
                        jvmArgs = listOf("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED")
                    }
                }
            }
        }
    }
}