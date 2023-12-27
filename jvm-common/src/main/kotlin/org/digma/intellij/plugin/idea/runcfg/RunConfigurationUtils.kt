package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.vcs.VcsService
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration


fun getWrapperFor(configuration: RunConfiguration, module: Module?): RunConfigurationWrapper? {
    return listOf(
        QuarkusRunConfigurationWrapper.getInstance(configuration.project), // quarkus is first so could catch unit tests before the standard AutoOtelAgent
        OpenLibertyRunConfigurationWrapper.getInstance(configuration.project),
        AutoOtelAgentRunConfigurationWrapper.getInstance(configuration.project),
        EeAppServerAtIdeaUltimateRunConfigurationWrapper.getInstance(configuration.project),
        TomcatRunConfigurationWrapperForIdeaUltimate.getInstance(configuration.project),
    ).firstOrNull {
        it.canWrap(configuration, module)
    }
}


fun getWrapperFor(configuration: RunConfiguration): RunConfigurationWrapper? {
    val module = tryResolveModule(configuration, null)
    return getWrapperFor(configuration, module)
}


//this is for java and maven run configurations. merge in case users have their own JAVA_TOOL_OPTIONS
fun mergeJavaToolOptions(project: Project, params: JavaParameters, myJavaToolOptions: String) {
    var javaToolOptions = myJavaToolOptions
    if (params.env.containsKey(JAVA_TOOL_OPTIONS)) {
        val currentJavaToolOptions = params.env[JAVA_TOOL_OPTIONS]
        javaToolOptions = "$myJavaToolOptions $currentJavaToolOptions"
    }
    params.env[JAVA_TOOL_OPTIONS] = javaToolOptions

    updateOtelResourceAttribute(project, params)
}


//this is only for gradle. we need to keep original JAVA_TOOL_OPTIONS if exists and restore when the process is
// finished, anyway we need to clean our JAVA_TOOL_OPTIONS because it will be saved in the run configuration settings.
fun mergeGradleJavaToolOptions(configuration: GradleRunConfiguration, myJavaToolOptions: String) {
    var javaToolOptions = myJavaToolOptions
    //need to replace the env because it may be immutable map
    val newEnv = configuration.settings.env.toMutableMap()
    if (configuration.settings.env.containsKey(JAVA_TOOL_OPTIONS)) {
        val currentJavaToolOptions = configuration.settings.env[JAVA_TOOL_OPTIONS]
        javaToolOptions = "$myJavaToolOptions $currentJavaToolOptions"
        newEnv[ORG_GRADLE_JAVA_TOOL_OPTIONS] = currentJavaToolOptions!!
    }
    newEnv[JAVA_TOOL_OPTIONS] = javaToolOptions
    configuration.settings.env = newEnv

    updateOtelResourceAttribute(configuration)
}


fun cleanGradleSettingsOnProcessEnd(configuration: RunConfigurationBase<*>) {
    configuration as GradleRunConfiguration
    if (configuration.settings.env.containsKey(ORG_GRADLE_JAVA_TOOL_OPTIONS)) {
        val newEnv = mutableMapOf<String, String>()
        val orgJavaToolOptions = configuration.settings.env[ORG_GRADLE_JAVA_TOOL_OPTIONS]
        newEnv.putAll(configuration.settings.env)
        if (orgJavaToolOptions != null) {
            newEnv[JAVA_TOOL_OPTIONS] = orgJavaToolOptions
        }
        newEnv.remove(ORG_GRADLE_JAVA_TOOL_OPTIONS)
        configuration.settings.env = newEnv
    } else if (configuration.settings.env.containsKey(JAVA_TOOL_OPTIONS)) {
        val newEnv = mutableMapOf<String, String>()
        newEnv.putAll(configuration.settings.env)
        newEnv.remove(JAVA_TOOL_OPTIONS)
        configuration.settings.env = newEnv
    }

    cleanOtelResourceAttributes(configuration)
}


fun updateOtelResourceAttribute(project: Project, params: JavaParameters) {

    val commitId = project.service<VcsService>().commitIdForCurrentProject

    val otelResourceAttributes = if (params.env.containsKey(OTEL_RESOURCE_ATTRIBUTES)) {
        params.env[OTEL_RESOURCE_ATTRIBUTES].plus(",")
    } else {
        ""
    }.plus("scm.commit.id=$commitId")

    params.env[OTEL_RESOURCE_ATTRIBUTES] = otelResourceAttributes

}


fun updateOtelResourceAttribute(configuration: GradleRunConfiguration) {

    val commitId = configuration.project.service<VcsService>().commitIdForCurrentProject

    val newEnv = configuration.settings.env.toMutableMap()
    val otelResourceAttributes = if (configuration.settings.env.containsKey(OTEL_RESOURCE_ATTRIBUTES)) {
        val currentOtelResourceAttributes = configuration.settings.env[OTEL_RESOURCE_ATTRIBUTES]
        newEnv[ORG_OTEL_RESOURCE_ATTRIBUTES] = currentOtelResourceAttributes
        currentOtelResourceAttributes.plus(",")
    } else {
        ""
    }.plus("scm.commit.id=$commitId")

    newEnv[OTEL_RESOURCE_ATTRIBUTES] = otelResourceAttributes
    configuration.settings.env = newEnv
}


fun cleanOtelResourceAttributes(configuration: GradleRunConfiguration) {

    if (configuration.settings.env.containsKey(ORG_OTEL_RESOURCE_ATTRIBUTES)) {
        val newEnv = mutableMapOf<String, String>()
        val orgOtelResourceAttributes = configuration.settings.env[ORG_OTEL_RESOURCE_ATTRIBUTES]
        newEnv.putAll(configuration.settings.env)
        if (orgOtelResourceAttributes != null) {
            newEnv[OTEL_RESOURCE_ATTRIBUTES] = orgOtelResourceAttributes
        }
        newEnv.remove(ORG_OTEL_RESOURCE_ATTRIBUTES)
        configuration.settings.env = newEnv
    } else if (configuration.settings.env.containsKey(OTEL_RESOURCE_ATTRIBUTES)) {
        val newEnv = mutableMapOf<String, String>()
        newEnv.putAll(configuration.settings.env)
        newEnv.remove(OTEL_RESOURCE_ATTRIBUTES)
        configuration.settings.env = newEnv
    }

}