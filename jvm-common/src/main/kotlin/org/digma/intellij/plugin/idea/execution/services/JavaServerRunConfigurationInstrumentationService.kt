package org.digma.intellij.plugin.idea.execution.services

import com.intellij.execution.configurations.RunConfiguration
import org.digma.intellij.plugin.execution.RunConfigurationType

//don't change to light service because it will register always. we want it to register only for Idea
@Suppress("LightServiceMigrationCode")
class JavaServerRunConfigurationInstrumentationService : BaseJvmRunConfigurationInstrumentationService() {

    private val supportedServerTypeIds: Set<String> = setOf(
        "JBossConfiguration", // also WildFly
        "GlassfishConfiguration",
        "TomeeConfiguration",
        "#com.intellij.j2ee.web.tomcat.TomcatRunConfigurationFactory"
    )

    override fun isApplicableFor(configuration: RunConfiguration): Boolean {
        return supportedServerTypeIds.contains(configuration.type.id) ||
                configuration.type.javaClass.simpleName == "TomcatConfiguration"
    }

    override fun getConfigurationType(configuration: RunConfiguration): RunConfigurationType {
        return if (isApplicableFor(configuration)) {
            RunConfigurationType.JavaSever
        } else {
            RunConfigurationType.Unknown
        }
    }
}