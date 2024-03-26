package org.digma.intellij.plugin.buildsystem

interface BuildSystemHelper {

    fun isBuildSystem(externalSystemId: String): Boolean
    fun getBuildSystem(externalSystemId: String): BuildSystem?

}