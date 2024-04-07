package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.ModuleRunConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleProgramParameters

open class ServiceNameProvider(
    protected val configuration: RunConfiguration,
    protected val params: SimpleProgramParameters
) {


    open fun provideServiceName(moduleResolver: ModuleResolver): String {

        val resolvedModule = moduleResolver.resolveModule()

        if (resolvedModule != null) {
            return trimName(resolvedModule.name)
        }


        if (configuration is ModuleRunConfiguration && configuration.modules.isNotEmpty()) {
            val moduleName = configuration.modules.firstOrNull()?.name
            if (moduleName != null) {
                return trimName(moduleName)
            }
        }

        return trimName(configuration.project.name)
    }


    private fun trimName(name: String): String {
        return name.replace(" ", "").trim()
    }

}