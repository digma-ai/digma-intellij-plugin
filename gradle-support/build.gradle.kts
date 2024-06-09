import common.dynamicPlatformType
import common.platformVersion
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("plugin-library")
}

//this module should always build with IC or IU
val platformType: IntelliJPlatformType by extra(dynamicPlatformType(project))


dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":jvm-common"))

    intellijPlatform {

        //this module can only build with IC or IU, so only support replacing to IU when
        // we build with IU , otherwise build with IC even if platformType is something else like RD or PY
        //although this module can always build with IC because it's the same gradle plugin, but just to be
        // sure to build with the gradle plugin that ships with the current IDE.
        if (platformType == IntelliJPlatformType.IntellijIdeaUltimate) {
            intellijIdeaUltimate(project.platformVersion())
        } else {
            intellijIdeaCommunity(project.platformVersion())
        }

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.plugins.gradle")
    }
}
