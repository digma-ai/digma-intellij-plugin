package org.digma.intellij.plugin.idea.buildsystem.helper

import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.buildsystem.BuildSystemHelper
import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.digma.intellij.plugin.idea.externalsystem.findMavenService

class MavenBuildSystemHelper : BuildSystemHelper {

    private val mavenService: BuildSystemHelperService? = findMavenService()

    override fun isBuildSystem(externalSystemId: String): Boolean {
        return mavenService?.isBuildSystem(externalSystemId) ?: false
    }

    override fun getBuildSystem(externalSystemId: String): BuildSystem? {
        if (isBuildSystem(externalSystemId)) {
            return BuildSystem.MAVEN
        }
        return null
    }
}