import common.platformVersion
import common.properties

plugins {
    id("plugin-library")
}

dependencies{
    compileOnly(project(":ide-common"))
}

intellij {
    version.set("CL-"+ platformVersion(project))
}
