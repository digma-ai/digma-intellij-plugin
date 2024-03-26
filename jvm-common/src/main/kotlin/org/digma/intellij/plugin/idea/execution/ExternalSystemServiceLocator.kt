package org.digma.intellij.plugin.idea.execution

import com.intellij.openapi.application.ApplicationManager
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService


fun findGradleService(): RunConfigurationInstrumentationService? {

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

fun findMavenService(): RunConfigurationInstrumentationService? {

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
