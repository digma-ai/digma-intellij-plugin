package org.digma.intellij.plugin.idea.externalsystem

import com.intellij.openapi.application.ApplicationManager
import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService


fun findGradleRunConfigurationInstrumentationService(): RunConfigurationInstrumentationService? {

    return try {
        val serviceClassName = "org.digma.intellij.plugin.idea.gradle.execution.GradleRunConfigurationService"
        val serviceClass = Class.forName(serviceClassName)
        @Suppress("IncorrectServiceRetrieving")
        ApplicationManager.getApplication().getService(serviceClass) as RunConfigurationInstrumentationService?
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("ExternalSystemServiceLocator.findGradleService", e)
        null
    }

}

fun findMavenRunConfigurationInstrumentationService(): RunConfigurationInstrumentationService? {

    return try {
        val serviceClassName = "org.digma.intellij.plugin.idea.maven.execution.MavenRunConfigurationService"
        val serviceClass = Class.forName(serviceClassName)
        @Suppress("IncorrectServiceRetrieving")
        ApplicationManager.getApplication().getService(serviceClass) as RunConfigurationInstrumentationService?
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("ExternalSystemServiceLocator.findMavenService", e)
        null
    }

}


fun findGradleService(): BuildSystemHelperService? {
    return try {
        val serviceClassName = "org.digma.intellij.plugin.idea.gradle.helper.GradleService"
        val serviceClass = Class.forName(serviceClassName)
        @Suppress("IncorrectServiceRetrieving")
        ApplicationManager.getApplication().getService(serviceClass) as BuildSystemHelperService?
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("GradleBuildSystemHelper.init", e)
        null
    }
}


fun findMavenService(): BuildSystemHelperService? {
    return try {
        val serviceClassName = "org.digma.intellij.plugin.idea.maven.helper.MavenService"
        val serviceClass = Class.forName(serviceClassName)
        @Suppress("IncorrectServiceRetrieving")
        ApplicationManager.getApplication().getService(serviceClass) as BuildSystemHelperService?
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("MavenBuildSystemHelper.init", e)
        null
    }
}
