package org.digma.intellij.plugin.idea.buildsystem.helper

import com.intellij.openapi.application.ApplicationManager
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.buildsystem.BuildSystemHelper
import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.digma.intellij.plugin.errorreporting.ErrorReporter

class MavenBuildSystemHelper : BuildSystemHelper {

    private val mavenService: BuildSystemHelperService? = try {
        val serviceClassName = "org.digma.intellij.plugin.idea.maven.helper.MavenService"
        val serviceClass = Class.forName(serviceClassName)
        @Suppress("IncorrectServiceRetrieving")
        ApplicationManager.getApplication().getService(serviceClass) as BuildSystemHelperService?
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("MavenBuildSystemHelper.init", e)
        null
    }

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