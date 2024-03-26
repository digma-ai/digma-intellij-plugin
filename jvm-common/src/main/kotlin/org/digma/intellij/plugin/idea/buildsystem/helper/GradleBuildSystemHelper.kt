package org.digma.intellij.plugin.idea.buildsystem.helper

import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.buildsystem.BuildSystemHelper
import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.digma.intellij.plugin.idea.externalsystem.findGradleService

class GradleBuildSystemHelper : BuildSystemHelper {

    private val gradleService: BuildSystemHelperService? = findGradleService()

    override fun isBuildSystem(externalSystemId: String): Boolean {
        return gradleService?.isBuildSystem(externalSystemId) ?: false
    }

    override fun getBuildSystem(externalSystemId: String): BuildSystem? {
        if (isBuildSystem(externalSystemId)) {
            return BuildSystem.GRADLE
        }
        return null
    }
}