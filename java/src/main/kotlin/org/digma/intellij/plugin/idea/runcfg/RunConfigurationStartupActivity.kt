package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.posthog.ActivityMonitor

class RunConfigurationStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val configurations = RunManager.getInstance(project).allConfigurationsList

        for(config in configurations){
            val module = RunCfgTools.tryResolveModule(config, null)
            val runConfigType = OtelRunConfigurationExtension.getWrapperFor(config, module)?.getRunConfigType(config, module)
            if(runConfigType == null)
                continue

            val taskNames = RunCfgTools.extractTasks(config)
            ActivityMonitor.getInstance(project).reportSupportedRunConfigDetected(runConfigType.name, taskNames)
        }
    }
}