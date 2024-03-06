package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.vcs.VcsService
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

private const val DIGMA_MARKER = "-Dorg.digma.marker=true"

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
@OptIn(JavaToolOptionsMergeUtils::class)
fun mergeJavaToolOptions(project: Project, params: JavaParameters, myJavaToolOptions: String) {

    var javaToolOptions = myJavaToolOptions
    val currentJavaToolOptions = params.env[JAVA_TOOL_OPTIONS]
    if (currentJavaToolOptions != null) {
        javaToolOptions = smartMergeJavaToolOptions(myJavaToolOptions, currentJavaToolOptions)
    }
    params.env[JAVA_TOOL_OPTIONS] = javaToolOptions

    updateOtelResourceAttribute(project, params)
}


//this is only for gradle. we need to keep original JAVA_TOOL_OPTIONS if exists and restore when the process is
// finished, anyway we need to clean our JAVA_TOOL_OPTIONS because it will be saved in the run configuration settings.
@OptIn(JavaToolOptionsMergeUtils::class)
fun mergeGradleJavaToolOptions(configuration: GradleRunConfiguration, myJavaToolOptions: String) {

    var javaToolOptions = myJavaToolOptions

    //need to replace the env because it may be immutable map
    val newEnv = configuration.settings.env.toMutableMap()

    val currentJavaToolOptions = newEnv[JAVA_TOOL_OPTIONS]

    if (currentJavaToolOptions != null) {

        //it's probably our JAVA_TOOL_OPTIONS that was not cleaned for some reason
        if (currentJavaToolOptions.trim().endsWith(DIGMA_MARKER)) {
            newEnv.remove(JAVA_TOOL_OPTIONS)
        } else {
            javaToolOptions = smartMergeJavaToolOptions(javaToolOptions, currentJavaToolOptions)
            newEnv[ORG_GRADLE_JAVA_TOOL_OPTIONS] = currentJavaToolOptions
        }
    }

    //mark the JavaToolOptions with DIGMA_MARKER, so that we know this it's ours, it helps protect against
    //a situation that we didn't clean the JAVA_TOOL_OPTIONS when the process started. we know it may happen sometimes
    // with GradleRunConfiguration.
    newEnv[JAVA_TOOL_OPTIONS] = javaToolOptions.plus(" $DIGMA_MARKER")
    configuration.settings.env = newEnv

    updateOtelResourceAttribute(configuration)
}


fun cleanGradleSettingsAfterProcessStart(configuration: GradleRunConfiguration) {
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

    val commitId = project.service<VcsService>().getCommitIdForCurrentProject()
        ?: return

    val otelResourceAttributes = if (params.env.containsKey(OTEL_RESOURCE_ATTRIBUTES)) {
        params.env[OTEL_RESOURCE_ATTRIBUTES].plus(",")
    } else {
        ""
    }.plus("scm.commit.id=$commitId")

    params.env[OTEL_RESOURCE_ATTRIBUTES] = otelResourceAttributes

}


fun alreadyHasDigmaEnvironmentInResourceAttribute(params: JavaParameters): Boolean {
    return params.env[OTEL_RESOURCE_ATTRIBUTES]?.contains("digma.environment=") ?: false
}


fun updateOtelResourceAttribute(configuration: GradleRunConfiguration) {

    val commitId = configuration.project.service<VcsService>().getCommitIdForCurrentProject()
        ?: return


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

fun alreadyHasDigmaEnvironmentInResourceAttributeInGradleConfig(configuration: GradleRunConfiguration): Boolean {
    return configuration.settings.env[OTEL_RESOURCE_ATTRIBUTES]?.contains("digma.environment=") ?: false
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


@JavaToolOptionsMergeUtils
@VisibleForTesting
internal fun smartMergeJavaToolOptions(myJavaToolOptions: String, currentJavaToolOptions: String): String {

    //merge two java tool options strings. values from myJavaToolOptions override values from currentJavaToolOptions

    val myOptions = javaToolOptionsToMap(myJavaToolOptions)
    val currentOptions = javaToolOptionsToMap(currentJavaToolOptions)

    val result = mutableMapOf<String, String?>()
    result.putAll(currentOptions)
    result.putAll(myOptions)

    return result.map { entry: Map.Entry<String, String?> ->
        if (entry.value.isNullOrBlank()) {
            entry.key
        } else {
            "${entry.key}=${entry.value}"
        }

    }.joinToString(" ")
}


@JavaToolOptionsMergeUtils
@VisibleForTesting
internal fun javaToolOptionsToMap(myJavaToolOptions: String): Map<String, String?> {

    //transform the list to a map.
    //considering only -D and agents as properties, all other options are considered as just an option.
    //handling -XX is complicated because some may be key value and some not,we don't add any -XX so if
    // use added some they will just be kept as is.

    val myList = myJavaToolOptions.split(" ")

    //associate preserves the entry iteration order of the original collection so the order of the options is preserved
    // and also ensures unit tests will always succeed.
    return myList.associate {
        val option = it.trim()
        if (option.startsWith("-D") ||
            option.startsWith("-javaagent") ||
            option.startsWith("-agentpath") ||
            option.startsWith("-agentlib")
        ) {
            val pair = option.split("=", limit = 2)
            if (pair.size == 1) {
                pair[0] to ""
            } else if (pair.size == 2) {
                pair[0] to pair[1]
            } else {
                it to null
            }
        } else {
            option to null
        }
    }
}

/**
 * ideally the two methods smartMergeJavaToolOptions and javaToolOptionsToMap should be private to this file
 * because they are very specific and should not be used elsewhere.
 * but if they are private they can't be used in unit tests.
 * Annotating these two methods with JavaToolOptionsMergeUtils prevents accidental use of them in other places.
 * in order to use them a method or class need to @OptIn(JavaToolOptionsMergeUtils::class), including the unit test class.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only to be used in RunConfigurationUtils")
@Retention(AnnotationRetention.BINARY)
annotation class JavaToolOptionsMergeUtils