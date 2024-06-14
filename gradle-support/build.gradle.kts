import common.dynamicPlatformType
import common.platformVersion
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("plugin-library")
}

//todo: modules that need to build with Idea can always use IC , there is no real need to build with IU
//this module should always build with IC or IU.
//if building with buildWithRider=true then this module should not use the dynamic type.
// it should use the dynamic type only when building with buildWIthUltimate=true
//platformType impacts project.platformVersion() so it must be accurate.
val platformType: IntelliJPlatformType by extra {
    if (dynamicPlatformType(project) == IntelliJPlatformType.IntellijIdeaUltimate){
        IntelliJPlatformType.IntellijIdeaUltimate
    }else{
        IntelliJPlatformType.IntellijIdeaCommunity
    }
}



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
