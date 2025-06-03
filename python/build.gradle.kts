import common.dynamicPlatformType
import common.platformVersion
import common.useBinaryInstaller
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

    testImplementation(project(":ide-common"))
    testImplementation(project(":model"))

    intellijPlatform {

        //see: https://plugins.jetbrains.com/docs/intellij/pycharm.html#python-plugins

        //we usually don't build this module with pycharmProfessional, only with pycharmCommunity.
        //it's possible to build the whole project with pycharmCommunity or pycharmProfessional by passing
        // -PbuildWithPycharm=true or -PbuildWithPycharmPro=true, doing that will build also the ide-common with
        // this IDE. this functionality is meant to replace plugin verifier as it will build all the common code
        // with this IDE. but if we run plugin verifier on pycharm then it is not really necessary.
        if (platformType == IntelliJPlatformType.PyCharmCommunity) {
            pycharmCommunity(project.platformVersion(), project.useBinaryInstaller())
            bundledPlugin("PythonCore")
        } else {
            //we don't use the functionality of pycharm pro, we keep everything compatible with pycharm community.
            //if we ever need to use the functionality of pycharm pro, then also PythonCore is necessary.
            pycharmProfessional(project.platformVersion(), project.useBinaryInstaller())
            bundledPlugin("PythonCore")
            bundledPlugin("Pythonid")
        }
    }
}