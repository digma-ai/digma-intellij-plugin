package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.posthog.ActivityMonitor

class RunConfigurationStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {


        try {
            val supported = mutableListOf<String>()
            val nonSupported = mutableListOf<String>()


            val allRunConfigs = RunManager.getInstance(project).allConfigurationsList
            val tempRunConfigs =
                RunManager.getInstance(project).tempConfigurationsList.map { runnerAndConfigurationSettings: RunnerAndConfigurationSettings -> runnerAndConfigurationSettings.configuration }

            collect(allRunConfigs, tempRunConfigs, supported, nonSupported)


            val details = mapOf(
                "Supported Run Configs Found" to supported.isNotEmpty(),
                "supported run configs" to supported.joinToString("; "),
                "non supported run configs" to nonSupported.joinToString("; ")
            )

            ActivityMonitor.getInstance(project).reportSupportedRunConfigDetected(details)

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "RunConfigurationStartupActivity.runActivity", e)
        }
    }

    private fun collect(
        configurations: List<RunConfiguration>?,
        tempConfigurations: List<RunConfiguration>?,
        supported: MutableList<String>,
        nonSupported: MutableList<String>,
    ) {

        configurations?.forEach { config ->

            if (config is RunConfigurationBase<*>) {

                val isTemp = isTempRunConfig(config, tempConfigurations)

                val handler = RunConfigurationHandlersHolder.sortedByOrder().find {
                    it.isHandlingType(config)
                }

                val taskNames = handler?.getTaskNames(config) ?: setOf()
                val list = if (handler == null) nonSupported else supported
                list.add(
                    "{${config.name} (type:${config.type.displayName}) (temp:$isTemp) [tasks:${taskNames.joinToString(",")}]}".replace(
                        "[tasks:]",
                        ""
                    )
                )
            }
        }
    }

    private fun isTempRunConfig(config: RunConfiguration, tempConfigurations: List<RunConfiguration>?): Boolean {
        tempConfigurations?.forEach {
            return it.name == config.name
        }
        return false
    }
}