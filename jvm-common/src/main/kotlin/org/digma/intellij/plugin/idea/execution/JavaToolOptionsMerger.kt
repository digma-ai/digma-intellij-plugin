package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.vcs.VcsService

open class JavaToolOptionsMerger(
    protected val configuration: RunConfiguration,
    protected val params: SimpleProgramParameters,
    @Suppress("unused")
    protected val parametersExtractor: ParametersExtractor
) {


    //default implementation that is probably ok for most configurations
    open fun mergeJavaToolOptions(instrumentedJavaToolOptions: String) {
        var javaToolOptions = instrumentedJavaToolOptions
        val currentJavaToolOptions = params.env[JAVA_TOOL_OPTIONS]
        if (currentJavaToolOptions != null) {
            javaToolOptions = smartMergeJavaToolOptions(javaToolOptions, currentJavaToolOptions)
        }
        params.env[JAVA_TOOL_OPTIONS] = javaToolOptions

        updateOtelResourceAttribute(configuration, params)
    }


    private fun updateOtelResourceAttribute(configuration: RunConfiguration, params: SimpleProgramParameters) {

        val commitId = VcsService.getInstance(configuration.project).getCommitIdForCurrentProject()
            ?: return

        val otelResourceAttributes = if (params.env.containsKey(OTEL_RESOURCE_ATTRIBUTES)) {
            params.env[OTEL_RESOURCE_ATTRIBUTES].plus(",")
        } else {
            ""
        }.plus("scm.commit.id=$commitId")

        params.env[OTEL_RESOURCE_ATTRIBUTES] = otelResourceAttributes
    }


    protected fun smartMergeJavaToolOptions(myJavaToolOptions: String, currentJavaToolOptions: String): String {

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


    private fun javaToolOptionsToMap(myJavaToolOptions: String): Map<String, String?> {

        //transform the list to a map.
        //considering only -D and agents as properties, all other options are considered as just an option.
        //handling -XX is complicated because some may be key value and some not,we don't add any -XX so if
        // there are any they will just be kept as is.

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
                @Suppress("CascadeIf")
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


}