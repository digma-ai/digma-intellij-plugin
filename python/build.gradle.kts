import common.currentProfile
import common.dynamicPlatformType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType


plugins {
    id("plugin-library")
}

//this module should always build with PC or PY
//if building with buildWithRider=true then this module should not use the dynamic type, only
// when building with buildWithPycharmPro=true
val platformType: IntelliJPlatformType by extra {
    if (dynamicPlatformType(project) == IntelliJPlatformType.PyCharmProfessional){
        IntelliJPlatformType.PyCharmProfessional
    }else{
        IntelliJPlatformType.PyCharmCommunity
    }
}


dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))

    testImplementation(project(":model"))

    intellijPlatform {

        //the python module can also be built with IC and a dependency on python plugin.
        //its better and more comfortable to just build with pycharm, in the past we had to build with IC
        //because github workflow were running out of disk space when building with pycharm because then a build
        //needed to download all IDEs. but we don't have this issue anymore so this module just builds with pycharm.


        //this module can only build with PC or PY, so only support replacing to PY when
        // we build with PY , otherwise build with PC even if platformType is something else like RD or IC
        if (platformType == IntelliJPlatformType.PyCharmProfessional) {
            pycharmProfessional(project.currentProfile().pycharmVersion)
        } else {
            pycharmCommunity(project.currentProfile().pycharmVersion)
        }

        //load plugin based on IDE PY or PC
        val pythonPlugin = if (platformType == IntelliJPlatformType.PyCharmProfessional) "Pythonid" else "PythonCore"
        bundledPlugin(pythonPlugin)
    }
}
