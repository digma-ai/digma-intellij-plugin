package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.ModuleRunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.module.Module

open class ServiceNameProvider(
    protected val configuration: RunConfigurationBase<*>,
    protected val params: SimpleProgramParameters
) {


    open fun provideServiceName(resolvedModule: Module?): String {

        if (resolvedModule != null) {
            return trimName(resolvedModule.name)
        }

        if (configuration is ModuleBasedConfiguration<*, *> &&
            configuration.configurationModule != null &&
            configuration.configurationModule.module != null
        ) {
            //another null check to satisfy kotlin
            val moduleName = configuration.configurationModule.module?.name
            if (moduleName != null) {
                return trimName(moduleName)
            }
        }


        if (params is SimpleJavaParameters && params.moduleName != null) {
            return trimName(params.moduleName)
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